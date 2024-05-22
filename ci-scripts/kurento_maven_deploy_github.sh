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

    log "INFO: Reading all versions of '$PROJECT_NAME' from GitHub."
    local API_VERSIONS_JSON; API_VERSIONS_JSON="$(
        curl -sS \
            -H "Accept: application/vnd.github+json" \
            -H "Authorization: Bearer $GITHUB_TOKEN" \
            -H "X-GitHub-Api-Version: 2022-11-28" \
            "https://api.github.com/orgs/Kurento/packages/maven/$PROJECT_NAME/versions"
    )"

    local API_VERSION_ID; API_VERSION_ID="$(echo "$API_VERSIONS_JSON" | jq ".[] | select(.name==\"$PROJECT_VERSION\")? | .id")"
    [[ -n "$API_VERSION_ID" ]] || {
        log "WARNING: Version '${PROJECT_NAME}:${PROJECT_VERSION}' not found in GitHub. Nothing to delete."
        return
    }

    curl -sS \
        -X DELETE \
        -H "Accept: application/vnd.github+json" \
        -H "Authorization: Bearer $GITHUB_TOKEN" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        "https://api.github.com/orgs/Kurento/packages/maven/$PROJECT_NAME/versions/$API_VERSION_ID"

    # If there was only a single instance of the given version, GitHub rejects
    # the deletion with an error message:
    # "You cannot delete the last version of a package. You must delete the package instead."
    # But the operation is still successful for our intents. We don't want to be
    # deleting and recreating the whole package, only its excess versions.

    log "INFO: Finished deleting versions of '${PROJECT_NAME}:${PROJECT_VERSION}' from GitHub."
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
MVN_DIRS=()
{
    MAVEN_CMD=(mvn "${MVN_ARGS[@]}" exec:exec -Dexec.executable=pwd)

    # 2024-05-22: This command was failing on CI, and errors were suppressed.
    # Adding an initial dry run, to be able to see errors in the output logs.
    log "DRY RUN. Showing Maven logs:"
    "${MAVEN_CMD[@]}"

    log "REAL RUN. Suppressing Maven logs, just get the result:"
    MAVEN_CMD+=(--quiet)
    # shellcheck disable=SC2207
    MVN_DIRS=($("${MAVEN_CMD[@]}")) || {
        log "ERROR: Command failed: mvn exec pwd"
        exit 1
    }
}

for MVN_DIR in "${MVN_DIRS[@]}"; do
    pushd "$MVN_DIR" || exit 1
    delete_github_version
    popd || exit 1
done

# And now, finally, deploy the package (and submodules, if any).
mvn "${MVN_ARGS[@]}" package "${MAVEN_DEPLOY_PLUGIN}:deploy"
