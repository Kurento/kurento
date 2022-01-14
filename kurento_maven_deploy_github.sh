#!/usr/bin/env bash



# Shell setup
# ===========

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Check dependencies.
command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
    exit 1
}

# Trace all commands.
set -o xtrace



# Parse call arguments
# ====================

#/ Arguments:
#/
#/   All: Command to run.
#/      Mandatory.

MVN_COMMAND=("$@")
[[ -z "${MVN_COMMAND:+x}" ]] && {
    log "ERROR: Missing argument(s): Maven command to run"
    exit 1
}



# Helper functions
# ================

# Requires $GITHUB_TOKEN with `read:packages` and `delete:packages` scopes.

function delete_github_version {
    local PROJECT_NAME; PROJECT_NAME="$(
        mvn --batch-mode --quiet --non-recursive \
            exec:exec -Dexec.executable=echo -Dexec.args='${project.groupId}.${project.artifactId}'
    )"

    local PROJECT_VERSION; PROJECT_VERSION="$(
        mvn --batch-mode --quiet --non-recursive \
            exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'
    )"

    log "INFO: Reading all versions of '${PROJECT_NAME}' from GitHub."
    local API_VERSIONS_JSON; API_VERSIONS_JSON="$(
        curl -sS \
            -H "Accept: application/vnd.github.v3+json" \
            -H "Authorization: token $GITHUB_TOKEN" \
            "https://api.github.com/orgs/kurento/packages/maven/$PROJECT_NAME/versions"
    )"

    local API_VERSION_ID; API_VERSION_ID="$(echo "$API_VERSIONS_JSON" | jq ".[] | select(.name==\"$PROJECT_VERSION\")? | .id")"
    [[ -n "$API_VERSION_ID" ]] || {
        log "WARNING: Version '${PROJECT_NAME}:${PROJECT_VERSION}' not found in GitHub. Nothing to delete."
        return
    }

    curl -sS \
        -X DELETE \
        -H "Accept: application/vnd.github.v3+json" \
        -H "Authorization: token $GITHUB_TOKEN" \
        "https://api.github.com/orgs/kurento/packages/maven/$PROJECT_NAME/versions/$API_VERSION_ID"

    log "INFO: Successfully deleted version '${PROJECT_NAME}:${PROJECT_VERSION}' from GitHub."
}



# Deploy to GitHub
# ================

MVN_ARGS=(
    --batch-mode
    --quiet
    -Dmaven.test.skip=true
    -Pdeploy
)

# Install packages into the local cache.
# We'll be deleting versions from the remote repository, so all dependencies
# must be already available locally when Maven runs.
mvn "${MVN_ARGS[@]}" clean install

# For each submodule, go into its path and delete the current GitHub version.
MVN_DIRS=($(mvn "${MVN_ARGS[@]}" exec:exec -Dexec.executable=pwd))
for MVN_DIR in "${MVN_DIRS[@]}"; do
    pushd "$MVN_DIR"
    delete_github_version
    popd
done

# And now, finally, deploy the package (and submodules, if any).
"${MVN_COMMAND[@]}"



log "==================== END ===================="
