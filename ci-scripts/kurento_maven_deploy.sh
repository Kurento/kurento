#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Deploy Java artifacts with Maven.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Check dependencies
# ==================

command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
    exit 1
}



# Settings
# ========

# Fully-qualified plugin names, to use newer versions than the Maven defaults.
MAVEN_DEPLOY_PLUGIN="org.apache.maven.plugins:maven-deploy-plugin:3.1.0"
MAVEN_JAVADOC_PLUGIN="org.apache.maven.plugins:maven-javadoc-plugin:3.5.0"
MAVEN_SOURCE_PLUGIN="org.apache.maven.plugins:maven-source-plugin:3.2.1"

# Maven arguments that are common to all commands.
MVN_ARGS=(
    --batch-mode
    --no-transfer-progress
    -Dmaven.test.skip=true
)



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



# Validate config
# ===============

if [[ -n "${MAVEN_SETTINGS_PATH:-}" ]]; then
    MVN_ARGS+=(--settings "$MAVEN_SETTINGS_PATH")
fi

if [[ -z "${MAVEN_SIGN_ARTIFACTS:-}" ]]; then
    MAVEN_SIGN_ARTIFACTS="true"
fi



# Deploy to local repo
# ====================

# First, make an initial build that gets deployed to a local repository.
# This is what gets archived by CI, and passed along to dependent jobs.
# The repo is set by the `ci-build` profile from Maven's `settings.xml`.

mvn "${MVN_ARGS[@]}" -Pci-build clean package "$MAVEN_DEPLOY_PLUGIN:deploy" || {
    log "ERROR: Command failed: mvn deploy (Jenkins repo)"
    exit 1
}



# Deploy to remote repo
# =====================

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
# {
#     GROUP_ID="$(mvn --quiet --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.groupId}')"
#     ARTIFACT_ID="$(mvn --quiet --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.artifactId}')"
#     PUBLIC_VERSION="$(curl --silent "https://search.maven.org/solrsearch/select?q=g:${GROUP_ID}+AND+a:${ARTIFACT_ID}&rows=20&wt=json" | jq --raw-output '.response.docs[0].latestVersion')"
#     if dpkg --compare-versions "$PROJECT_VERSION" le "$PUBLIC_VERSION"; then
#         log "Skip deploying: project version <= public version"
#         exit 0
#     fi
# }

MVN_ARGS+=(-Pkurento-release)

if [[ $MAVEN_SIGN_ARTIFACTS == "true" ]]; then
    log "Artifact signing on deploy is ENABLED"

    MVN_ARGS+=(-Pgpg-sign)

    MVN_GOALS=(
        package
        "$MAVEN_SOURCE_PLUGIN:jar"
        "$MAVEN_JAVADOC_PLUGIN:jar"
        gpg:sign
        "$MAVEN_DEPLOY_PLUGIN:deploy"
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

    MVN_GOALS=(
        package
        "$MAVEN_SOURCE_PLUGIN:jar"
        "$MAVEN_JAVADOC_PLUGIN:jar"
        "$MAVEN_DEPLOY_PLUGIN:deploy"
    )

    mvn "${MVN_ARGS[@]}" "${MVN_GOALS[@]}" || {
        log "ERROR: Command failed: mvn deploy (unsigned release)"
        exit 1
    }
fi
