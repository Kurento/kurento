#!/usr/bin/env bash



# Shell setup
# ===========

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands.
set -o xtrace



# MAVEN_SETTINGS path
#   Path to settings.xml file used by maven
#
# SNAPSHOT_REPOSITORY url
#   Repository used to deploy snapshot artifacts.
#   If empty, will use Maven settings from `<distributionManagement>`.
#
# RELEASE_REPOSITORY url
#   Repository used to deploy release artifacts.
#   If empty, will use Maven settings from `<distributionManagement>`.
#
# SIGN_ARTIFACTS true | false
#   Whether to sign artifacts before deployment. Default value is true
#

# Path information
# BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# PATH="${BASEPATH}:${PATH}"

# Get command line parameters for backward compatibility
# [[ -n "${1:-}" ]] && MAVEN_SETTINGS="$1"
# [[ -n "${2:-}" ]] && {
#     SNAPSHOT_REPOSITORY="$2"
#     RELEASE_REPOSITORY="$2"
# }
# [[ -n "${3:-}" ]] && SIGN_ARTIFACTS="$3"

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

# needed env vars
# export AWS_ACCESS_KEY_ID="$UBUNTU_PRIV_S3_ACCESS_KEY_ID"
# export AWS_SECRET_ACCESS_KEY="$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID"

# Maven arguments that are common to all commands.
MVN_ARGS+=(
    --batch-mode
    -Dmaven.test.skip=true
    # -Dmaven.wagon.http.ssl.insecure=true
    # -Dmaven.wagon.http.ssl.allowall=true
    -Pdeploy
)

# Intermediate Maven goals that should run before `deploy`.
MVN_GOALS=(clean)

PROJECT_VERSION="$(kurento_get_version.sh)" || {
  log "ERROR: Command failed: kurento_get_version"
  exit 1
}
log "Build and deploy version: $PROJECT_VERSION"

if [[ $PROJECT_VERSION == *-SNAPSHOT ]]; then
    log "Version to deploy is SNAPSHOT"

    # if [[ -n "${SNAPSHOT_REPOSITORY:-}" ]]; then
    #     MVN_ARGS+=(
    #         -DaltSnapshotDeploymentRepository="$SNAPSHOT_REPOSITORY"
    #     )
    # fi

    MVN_CMD=(mvn)
    MVN_CMD+=("${MVN_ARGS[@]}")
    MVN_CMD+=("${MVN_GOALS[@]}")
    MVN_CMD+=(deploy)

    kurento_maven_deploy_github.sh "${MVN_CMD[@]}" || {
        log "ERROR: Command failed: kurento_maven_deploy_github"
        exit 1
    }
else
    log "Version to deploy is RELEASE"

    MVN_ARGS+=(-Pkurento-release)

    # if [[ -n "${RELEASE_REPOSITORY:-}" ]]; then
    #     MVN_ARGS+=(
    #         -DaltReleaseDeploymentRepository="$RELEASE_REPOSITORY"
    #     )
    # fi

    MVN_GOALS+=(javadoc:jar source:jar)

    if [[ $SIGN_ARTIFACTS == "true" ]]; then
        log "Artifact signing on deploy is ENABLED"

        MVN_ARGS+=(-Pgpg-sign)

        MVN_GOALS+=(gpg:sign)

        mvn "${MVN_ARGS[@]}" "${MVN_GOALS[@]}" deploy || {
            log "ERROR: Command failed: mvn deploy (signed release)"
            exit 1
        }

        #Verify signed files (if any)
        SIGNED_FILES=$(find ./target -type f | egrep '\.asc$')

        [[ -z "$SIGNED_FILES" ]] && {
            log "Exit: No signed files found"
            exit 0
        }

        for FILE in $SIGNED_FILES; do
            SIGNED_FILE="$(echo "$FILE" | sed 's/.asc\+$//')"
            gpg --verify "$FILE" "$SIGNED_FILE" || {
                log "ERROR: Command failed: gpg verify"
                exit 1
            }
        done
    else
        log "Artifact signing on deploy is DISABLED"

        mvn "${MVN_ARGS[@]}" "${MVN_GOALS[@]}" deploy || {
            log "ERROR: Command failed: mvn deploy (unsigned release)"
            exit 1
        }
    fi
fi



log "==================== END ===================="
