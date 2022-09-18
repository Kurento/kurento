#!/usr/bin/env bash



# Shell setup
# ===========

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands.
set -o xtrace



# Check dependencies
# ==================

command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
    exit 1
}



# Verify project
# ==============

[[ -f pom.xml ]] || {
    log "ERROR: File not found: pom.xml"
    exit 1
}

kurento_check_version.sh false || {
    log "ERROR: Command failed: kurento_check_version (tagging disabled)"
    exit 1
}



# MAVEN_SETTINGS path
#   Path to settings.xml file used by maven
#
# SIGN_ARTIFACTS true | false
#   Whether to sign artifacts before deployment. Default value is true
#

# Path information
# BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# PATH="${BASEPATH}:${PATH}"

MVN_ARGS=()

# Validate parameters
log "Validate parameters"
if [[ -n "$MAVEN_SETTINGS" ]]; then
    [[ -f "$MAVEN_SETTINGS" ]] || {
        log "ERROR: Cannot read file: $MAVEN_SETTINGS"
        exit 1
    }
    MVN_ARGS+=(--settings "$MAVEN_SETTINGS")
fi
[[ -z "${SIGN_ARTIFACTS:-}" ]] && SIGN_ARTIFACTS="true"

# Maven arguments that are common to all commands.
MVN_ARGS+=(
    --batch-mode
    -Dmaven.test.skip=true
)

# Fully-qualified goal name for the "deploy" plugin.
# Needed so that we can use newer versions than the Maven default.
MVN_GOAL_DEPLOY="org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M2:deploy"

# First, make an initial build that gets deployed to a local repository. This is
# archived by Jenkins, and passed along to dependent jobs.
# The repo is set by the `ci-build` profile from Maven's `settings.xml`.
mvn "${MVN_ARGS[@]}" -Pci-build clean package "$MVN_GOAL_DEPLOY" || {
    log "ERROR: Command failed: mvn deploy (Jenkins repo)"
    exit 1
}

# Now make the actual deployment.

MVN_ARGS+=(
    -Pdeploy
)

PROJECT_VERSION="$(kurento_get_version.sh)" || {
  log "ERROR: Command failed: kurento_get_version"
  exit 1
}
log "Build and deploy version: $PROJECT_VERSION"

# If SNAPSHOT, deploy to snapshots repository and exit.
if [[ $PROJECT_VERSION == *-SNAPSHOT ]]; then
    log "Version to deploy is SNAPSHOT"

    MVN_ARGS+=(-Psnapshot)

    source kurento_maven_deploy_github.sh || {
        log "ERROR: Command failed: kurento_maven_deploy_github"
        exit 1
    }

    exit 0
fi

log "Version to deploy is RELEASE"

# Don't deploy versions not greater than what is already published.
{
    GROUP_ID="$(mvn --quiet --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.groupId}')"
    ARTIFACT_ID="$(mvn --quiet --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.artifactId}')"
    PUBLIC_VERSION="$(curl --silent "https://search.maven.org/solrsearch/select?q=g:${GROUP_ID}+AND+a:${ARTIFACT_ID}&rows=20&wt=json" | jq --raw-output '.response.docs[0].latestVersion')"

    if dpkg --compare-versions "$PROJECT_VERSION" le "$PUBLIC_VERSION"; then
        log "Skip deploying: project version <= public version"
        exit 0
    fi
}

MVN_ARGS+=(-Pkurento-release)

MVN_GOALS=(
    javadoc:jar
    source:jar
)

if [[ $SIGN_ARTIFACTS == "true" ]]; then
    log "Artifact signing on deploy is ENABLED"

    MVN_ARGS+=(-Pgpg-sign)

    MVN_GOALS+=(
        package
        gpg:sign # "sign" requires an already packaged artifact.
        "$MVN_GOAL_DEPLOY"
    )

    mvn "${MVN_ARGS[@]}" "${MVN_GOALS[@]}" || {
        log "ERROR: Command failed: mvn deploy (signed release)"
        exit 1
    }

    # Verify signed files (if any)
    mapfile -t SIGNED_FILES < <(find ./target -type f -name '*.asc' ! -name '*.asc.asc')

    if [[ ${#SIGNED_FILES[@]} -eq 0 ]]; then
        log "Exit: No signed files found"
        exit 0
    fi

    log "Signed files:"
    printf '  %s\n' "${SIGNED_FILES[@]}"

    for SIGNED_FILE in "${SIGNED_FILES[@]}"; do
        FILE="${SIGNED_FILE%.asc}"
        gpg --verify "$SIGNED_FILE" "$FILE" || {
            log "ERROR: Command failed: gpg --verify '$SIGNED_FILE' '$FILE'"
            exit 1
        }
    done
else
    log "Artifact signing on deploy is DISABLED"

    MVN_GOALS+=(
        package
        "$MVN_GOAL_DEPLOY"
    )

    mvn "${MVN_ARGS[@]}" "${MVN_GOALS[@]}" || {
        log "ERROR: Command failed: mvn deploy (unsigned release)"
        exit 1
    }
fi
