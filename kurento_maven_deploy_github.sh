#!/usr/bin/env bash

# Extension for `kurento_maven_deploy`, to deploy SNAPSHOT versions in the
# Kurento GitHub repository for Maven artifacts.



# Helper functions
# ================

# Requires $GITHUB_TOKEN with `read:packages` and `delete:packages` scopes.

function delete_github_version {
    local GROUPID; GROUPID="$(mvn --batch-mode --quiet help:evaluate -Dexpression=project.groupId -DforceStdout)"
    local ARTIFACTID; ARTIFACTID="$(mvn --batch-mode --quiet help:evaluate -Dexpression=project.artifactId -DforceStdout)"
    local PROJECT_NAME="${GROUPID}.${ARTIFACTID}"

    local PROJECT_VERSION; PROJECT_VERSION="$(mvn --batch-mode --quiet help:evaluate -Dexpression=project.version -DforceStdout)"

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

# Install packages into the local cache.
# We'll be deleting versions from the remote repository, so all dependencies
# must be already available locally when Maven runs.
mvn "${MVN_ARGS[@]}" install || {
    log "ERROR: Command failed: mvn install"
    exit 1
}

# For each submodule, go into its path and delete the current GitHub version.
# shellcheck disable=SC2207
MVN_DIRS=( $(mvn "${MVN_ARGS[@]}" --quiet exec:exec -Dexec.executable=pwd) ) || {
    log "ERROR: Command failed: mvn exec pwd"
    exit 1
}
for MVN_DIR in "${MVN_DIRS[@]}"; do
    pushd "$MVN_DIR" || exit 1
    delete_github_version
    popd || exit 1
done

# And now, finally, deploy the package (and submodules, if any).
mvn "${MVN_ARGS[@]}" package "$MVN_GOAL_DEPLOY"
