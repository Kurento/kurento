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



# Parse call arguments
# ====================

CFG_MAVEN_SETTINGS_PATH=""
CFG_MAVEN_SIGN_KEY_PATH=""
CFG_MAVEN_SIGN_ARTIFACTS="true"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --maven-settings)
            if [[ -n "${2-}" ]]; then
                CFG_MAVEN_SETTINGS_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --maven-settings expects <Path>"
                exit 1
            fi
            ;;
        --maven-sign-key)
            if [[ -n "${2-}" ]]; then
                CFG_MAVEN_SIGN_KEY_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --maven-sign-key expects <Path>"
                exit 1
            fi
            ;;
        --no-sign-artifacts)
            CFG_MAVEN_SIGN_ARTIFACTS="false"
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

if [[ "$CFG_MAVEN_SIGN_ARTIFACTS" == "true" ]] && [[ -z "$CFG_MAVEN_SIGN_KEY_PATH" ]]; then
    log "ERROR: Either of '--maven-sign-key' or '--no-sign-artifacts' must be used"
    exit 1
fi

log "CFG_MAVEN_SETTINGS_PATH=$CFG_MAVEN_SETTINGS_PATH"
log "CFG_MAVEN_SIGN_KEY_PATH=$CFG_MAVEN_SIGN_KEY_PATH"
log "CFG_MAVEN_SIGN_ARTIFACTS=$CFG_MAVEN_SIGN_ARTIFACTS"



# Verify project
# ==============

[[ -f pom.xml ]] || {
    log "ERROR: File not found: pom.xml"
    exit 1
}

CHECK_VERSION_ARGS=()
if [[ -n "${CFG_MAVEN_SETTINGS_PATH:-}" ]]; then
    CHECK_VERSION_ARGS+=(--maven-settings "$CFG_MAVEN_SETTINGS_PATH")
fi

check_version.sh "${CHECK_VERSION_ARGS[@]}" || {
    log "ERROR: Command failed: check_version.sh"
    exit 1
}



# Maven call arguments
# ====================

MVN_ARGS=()

# Arguments that are common to all commands.
MVN_ARGS+=(
    --batch-mode
    --no-transfer-progress
    -Dmaven.test.skip=true
)

# Path to the Maven settings.xml file.
if [[ -n "${CFG_MAVEN_SETTINGS_PATH:-}" ]]; then
    MVN_ARGS+=(--settings "$CFG_MAVEN_SETTINGS_PATH")
fi



# Deploy to local repo
# ====================

# First, make an initial build that gets deployed to a local repository.
# This is what gets archived by CI, and passed along to dependent jobs.
# The repo is set by the `deploy-local` profile from Maven's `settings.xml`.

mvn "${MVN_ARGS[@]}" -Pdeploy-local clean package "$MAVEN_DEPLOY_PLUGIN:deploy" || {
    log "ERROR: Command failed: mvn deploy (local repo)"
    exit 1
}



# Deploy to remote repo
# =====================

# Now, do an actual deployment to remote repositories. These are set by the
# `deploy` profile from Maven's `settings.xml`.

MVN_ARGS+=(
    -Pdeploy
)

GET_VERSION_ARGS=("${CHECK_VERSION_ARGS[@]}")

PROJECT_VERSION="$(get_version.sh "${GET_VERSION_ARGS[@]}")" || {
    log "ERROR: Command failed: get_version.sh"
    exit 1
}

log "Build and deploy version: $PROJECT_VERSION"

# Always deploy to snapshots repository.
# Having all versions (snapshot or not) in the snapshots repository can be handy
# for development when trying to test a release version but not wanting to wait
# until Maven Central makes the published packages available (which can take
# 30 or more minutes).
{
    MVN_ARGS+=(-Psnapshot)

    source maven_deploy_github.sh || {
        log "ERROR: Command failed: maven_deploy_github.sh"
        exit 1
    }
}

if [[ $PROJECT_VERSION == *-SNAPSHOT ]]; then
    log "Version to deploy is SNAPSHOT"
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

if [[ $CFG_MAVEN_SIGN_ARTIFACTS == "true" ]]; then
    log "Artifact signing on deploy is ENABLED"

    {
        # Load the private key into the GPG keyring.
        export GNUPGHOME="/tmp/.gnupg"
        gpg --import "$CFG_MAVEN_SIGN_KEY_PATH"
    }

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

# Setup maven central authorization token
TOKEN=$(printf "${KURENTO_MAVEN_SONATYPE_USERNAME}:${KURENTO_MAVEN_SONATYPE_PASSWORD}" | base64)

# Get open repositories
RESPONSE=$(curl -H "Authorization: Bearer $TOKEN" https://ossrh-staging-api.central.sonatype.com/manual/search/repositories)
if [ $? -ne 0 ]; then
    log "Error finding staging repositories"
    exit 1
fi
REPO_COUNT=$(echo "$RESPONSE" | jq -r '.repositories | length')
if [ "$REPO_COUNT" -eq 0 ]; then
    log "Cannot find any open staging repository"
    exit 1
fi
REPO_KEY=$(echo "$RESPONSE" | jq -r '.repositories[0].key')
DEPLOYMENT_ID=$(echo "$RESPONSE" | jq -r '.repositories[0].portal_deployment_id')
if [ -z "$REPO_KEY" ] || [ "$REPO_KEY" = "null" ]; then
    log "Failed to extract the repository key"
    log "API response: $RESPONSE"
    exit 1
fi
log "Staging repository found:"
log "  Repository Key: $REPO_KEY"
log "  Deployment ID: $DEPLOYMENT_ID"
echo ""

# Uploading repository top deployment portal URL
UPLOAD_RESPONSE=$(curl -s -i -X POST \
    -H "Authorization: Bearer $TOKEN" \
    "https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/${REPO_KEY}?publishing_type=user_managed")

HTTP_STATUS=$(echo "$UPLOAD_RESPONSE" | grep -i "^HTTP" | tail -1 | awk '{print $2}')

if [ "$HTTP_STATUS" -ne 200 ] && [ "$HTTP_STATUS" -ne 201 ]; then
    log "Error uploading staging repository"
    log "HTTP Status: $HTTP_STATUS"
    log "Response: $UPLOAD_RESPONSE"
    exit 1
fi
log "Staging repository uploaded successfully"
log "Complete publishing on https://central.sonatype.com/publishing/deployments"
