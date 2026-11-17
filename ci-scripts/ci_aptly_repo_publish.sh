#!/usr/bin/env bash
# Always use ShellCheck! -- https://www.shellcheck.net/

#/ CI - Create or update Aptly repos to store Debian packages.
#/
#/ This script will handle all Aptly operations to create or update already
#/ existing repositories.
#/
#/ The script is meant to run in the remote server that hosts Aptly repos. It
#/ won't work if you run this script locally or in the Jenkins machine; instead,
#/ you should copy it to the target server and run remotely via SSH.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --distro-name <DistroName>
#/
#/   Name of the Ubuntu distribution for which the repo will be created or
#/   updated. E.g.: "focal".
#/
#/   Required. Default: None.
#/
#/ --repo-name <RepoName>
#/
#/   Name of the repository that will be created or updated.
#/   E.g.: "kurento-focal-7.0.0", "kurento-focal-feature-branch"
#/
#/   Required. Default: None.
#/
#/ --publish-name <PublishName>
#/
#/   Name of the Aptly publishing endpoint that should be used.
#/   E.g.: "7.0.0", "test-branch"
#/
#/   The repository URL will be like:
#/
#/       http://ubuntu.openvidu.io/<PublishName> <DistroName> main
#/
#/   Required. Default: None.
#/
#/ --release
#/
#/   Create release snapshot repositories, to hold all packages that have been
#/   generated as part of a project release procedure.
#/
#/   If this option is not given, repositories are created or updated to hold
#/   nightly snapshot versions of the packages.
#/
#/   Optional. Default: Disabled.
#/
#/
#/
#/ How to *drop* and remove repos
#/ ==============================
#/
#/ This section should probably be made into a script itself, something like
#/ kurento_ci_aptly_repo_drop.sh
#/
#/ There are 3 things to drop:
#/
#/ * Publications (`aptly publish drop`)
#/ * Snapshots (`aptly snapshot drop`)
#/ * Repositories (`aptly repo drop`)
#/
#/
#/ Drop publication
#/ ----------------
#/
#/ Doc: https://www.aptly.info/doc/aptly/publish/
#/
#/ aptly publish list -raw
#/ aptly publish drop focal s3:ubuntu:bionic-gstreamer
#/
#/
#/ Drop snapshot
#/ -------------
#/
#/ WARNING: Normally you wouldn't need to drop any snapshot. Snapshots are only
#/ created for official releases, and you should really not be looking to delete
#/ any previous release.
#/
#/ Doc: https://www.aptly.info/doc/aptly/snapshot/
#/
#/ aptly snapshot list -raw
#/ aptly snapshot drop snap-kurento-xenial-1.2.3
#/
#/
#/ Drop repository
#/ ---------------
#/
#/ Doc: https://www.aptly.info/doc/aptly/repo/
#/
#/ aptly repo list -raw
#/ aptly repo drop kurento-focal-exp-bionic-gstreamer
#/
#/
#/ Database cleanup
#/ ----------------
#/
#/ Doc: https://www.aptly.info/doc/aptly/db/cleanup/
#/
#/ After deleting a repository, it is a good idea to perform a cleanup.
#/
#/ aptly db cleanup -verbose -dry-run
#/ aptly db cleanup -verbose


# Configure shell
# ===============

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace


# Check dependencies
# ==================

# Bash Associative Array: [command_name]="package_name"
declare -A DEP_COMMANDS=(
    [aptly]="https://www.aptly.info/download/"
    [flock]="https://github.com/util-linux/util-linux"
)
for DEP_COMMAND in "${!DEP_COMMANDS[@]}"; do
    command -v "$DEP_COMMAND" >/dev/null || {
        echo "[$0] ERROR: $DEP_COMMAND: command not found (please install from '${DEP_COMMANDS[$DEP_COMMAND]}')" >&2
        exit 1
    }
done


# Parse arguments
# ===============

CFG_DISTRO_NAME=""
CFG_REPO_NAME=""
CFG_PUBLISH_NAME=""
CFG_RELEASE="false"
CFG_ARCHITECTURES=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --distro-name)
            if [[ -n "${2-}" ]]; then
                CFG_DISTRO_NAME="$2"
                shift
            else
                echo "ERROR: --distro-name expects <DistroName>" >&2
                echo "Run with '--help' to read usage details" >&2
                exit 1
            fi
            ;;
        --repo-name)
            if [[ -n "${2-}" ]]; then
                CFG_REPO_NAME="$2"
                shift
            else
                echo "ERROR: --repo-name expects <RepoName>" >&2
                echo "Run with '--help' to read usage details" >&2
                exit 1
            fi
            ;;
        --publish-name)
            if [[ -n "${2-}" ]]; then
                CFG_PUBLISH_NAME="$2"
                shift
            else
                echo "ERROR: --publish-name expects <PublishName>" >&2
                echo "Run with '--help' to read usage details" >&2
                exit 1
            fi
            ;;
        --architectures)
            if [[ -n "${2-}" ]]; then
                CFG_ARCHITECTURES="$2"
                shift
            else
                echo "ERROR: --architectures expects <Architectures>" >&2
                echo "Run with '--help' to read usage details" >&2
                exit 1
            fi
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        *)
            echo "ERROR: Unknown argument '${1-}'" >&2
            echo "Run with '--help' to read usage details" >&2
            exit 1
            ;;
    esac
    shift
done


# Config restrictions
# ===================

if [[ -z "$CFG_DISTRO_NAME" ]]; then
    echo "ERROR: Missing --distro-name <DistroName>" >&2
    exit 1
fi

if [[ -z "$CFG_REPO_NAME" ]]; then
    echo "ERROR: Missing --repo-name <RepoName>" >&2
    exit 1
fi

if [[ -z "$CFG_PUBLISH_NAME" ]]; then
    echo "ERROR: Missing --publish-name <PublishName>" >&2
    exit 1
fi

echo "CFG_DISTRO_NAME=$CFG_DISTRO_NAME" >&2
echo "CFG_REPO_NAME=$CFG_REPO_NAME" >&2
echo "CFG_PUBLISH_NAME=$CFG_PUBLISH_NAME" >&2
echo "CFG_RELEASE=$CFG_RELEASE" >&2


# Step 1: Create repo
# ===================

APTLY_OUTPUT="$(aptly repo list -raw)"
if ! echo "$APTLY_OUTPUT" | grep --quiet --line-regexp "$CFG_REPO_NAME"; then
    echo "Create new repo: $CFG_REPO_NAME" >&2
    aptly repo create -distribution="$CFG_DISTRO_NAME" -component=main "$CFG_REPO_NAME"
fi


# Step 2: Add files to repo
# =========================

aptly repo add -force-replace -remove-files "$CFG_REPO_NAME" ./*.*deb


# Step 3: Publish repo
# ====================

# The storage can be overridden (e.g. "filesystem:ubuntu") to test locally.
PUBLISH_ENDPOINT="${APTLY_PUBLISH_STORAGE:-s3:ubuntu}:$CFG_PUBLISH_NAME"

ARCH_FLAG=""
if [[ -n "$CFG_ARCHITECTURES" ]]; then
    ARCH_FLAG="-architectures=$CFG_ARCHITECTURES"
fi

if [[ "$CFG_RELEASE" == "true" ]]; then
    # Aptly docs:
    # > It is not recommended to publish local repositories directly unless the
    # > repository is for testing purposes and changes happen frequently. For
    # > production usage please take snapshot of repository and publish it.
    SNAP_NAME="snap-$CFG_REPO_NAME"

    echo "Create and publish new release snapshot: $SNAP_NAME" >&2
    aptly snapshot create "$SNAP_NAME" from repo "$CFG_REPO_NAME"
    aptly publish snapshot -gpg-key="$GPGKEY" $ARCH_FLAG "$SNAP_NAME" "$PUBLISH_ENDPOINT"
else
    # Development repos are published straight from the local repo.
    #
    # Aptly 1.5 cannot change the architecture set of an existing publication
    # ('publish update' and 'publish switch' ignore -architectures, and 'publish
    # repo' refuses an endpoint/distribution already in use), so adding an
    # architecture later (e.g. arm64 on a repo first published as amd64-only)
    # requires dropping and publishing again. To make that safe:
    #
    # - Runs are serialized with a lock, so two jobs never race on the drop.
    # - The architecture set to keep is the UNION of what is published now,
    #   what a previous run recorded in a state file, and what is requested.
    #   Architectures are never removed, even if a run asks for a subset.
    # - If the published architectures cannot be read, nothing is touched.
    # - The union is recorded in the state file BEFORE dropping, so if a run is
    #   killed in the window between drop and publish, the next run publishes
    #   the full set again.
    # - The drop uses -skip-cleanup (pool files are kept) and the publish is
    #   retried; if it still fails, the previous publication is restored.
    STATE_DIR="${APTLY_PUBLISH_STATE_DIR:-$HOME/.aptly-publish-state}"
    STATE_FILE="$STATE_DIR/${PUBLISH_ENDPOINT//[^A-Za-z0-9._-]/_}__$CFG_DISTRO_NAME.archs"
    mkdir -p "$STATE_DIR"

    # Serialize concurrent runs for the same endpoint.
    exec 9>"$STATE_FILE.lock"
    if ! flock --wait 1800 9; then
        echo "ERROR: Timeout waiting for the publish lock: $STATE_FILE.lock" >&2
        exit 1
    fi

    # Normalize a comma/space separated list: lowercase, no duplicates, one line.
    normalize_archs() {
        echo "$*" | tr 'A-Z,' 'a-z ' | tr -s '[:space:]' '\n' | awk 'NF && !seen[$0]++' | paste -sd' '
    }

    # Publish the local repo to the endpoint with the given architectures
    # (comma-separated); succeeds only if the publication exists afterwards.
    publish_dev_repo() {
        aptly publish repo -gpg-key="$GPGKEY" -force-overwrite ${1:+-architectures="$1"} \
            "$CFG_REPO_NAME" "$PUBLISH_ENDPOINT"
    }

    # Set while the endpoint is dropped and not yet published again.
    DROPPED="false"
    PREVIOUS_ARCHS=""
    restore_on_exit() {
        local rc=$?
        trap - EXIT
        if [[ "$DROPPED" == "true" ]]; then
            echo "WARNING: Publication was dropped and not completed; restoring architectures: $PREVIOUS_ARCHS" >&2
            if publish_dev_repo "${PREVIOUS_ARCHS// /,}"; then
                echo "Previous architectures restored; the state file keeps the full set for the next run." >&2
            else
                echo "ERROR: Could not restore the publication! The next run will publish it again using $STATE_FILE" >&2
            fi
        fi
        exit "$rc"
    }
    trap restore_on_exit EXIT
    trap 'exit 143' TERM
    trap 'exit 130' INT HUP

    REQUESTED_ARCHS="$(normalize_archs "$CFG_ARCHITECTURES")"
    RECORDED_ARCHS=""
    if [[ -f "$STATE_FILE" ]]; then
        RECORDED_ARCHS="$(normalize_archs "$(cat "$STATE_FILE")")"
    fi

    # Read the publication state. A failure here (e.g. aptly DB locked by
    # another process) must NOT be interpreted as "not published".
    if ! PUBLISH_LIST="$(aptly publish list -raw)"; then
        echo "ERROR: 'aptly publish list' failed; not touching the publication" >&2
        exit 1
    fi

    if ! echo "$PUBLISH_LIST" | grep --quiet --line-regexp "$PUBLISH_ENDPOINT $CFG_DISTRO_NAME"; then
        # Not published (or dropped by an interrupted run): publish the union.
        FIRST_ARCHS="$(normalize_archs "$RECORDED_ARCHS $REQUESTED_ARCHS")"
        echo "Publish new development repo: $CFG_REPO_NAME (architectures: ${FIRST_ARCHS:-<default>})" >&2
        publish_dev_repo "${FIRST_ARCHS// /,}"
        echo "$FIRST_ARCHS" > "$STATE_FILE"
    else
        NEED_REPUBLISH="false"
        if [[ -n "$REQUESTED_ARCHS$RECORDED_ARCHS" ]]; then
            if ! SHOW_OUTPUT="$(aptly publish show "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT")"; then
                echo "ERROR: 'aptly publish show' failed; not touching the publication" >&2
                exit 1
            fi
            # Output line looks like: "Architectures: amd64 arm64 source"
            PREVIOUS_ARCHS="$(normalize_archs "$(echo "$SHOW_OUTPUT" | sed -n 's/^Architectures:[[:space:]]*//p')")"
            echo "Published architectures: ${PREVIOUS_ARCHS:-<none>}; recorded: ${RECORDED_ARCHS:-<none>}; requested: ${REQUESTED_ARCHS:-<none>}" >&2
            if [[ -z "$PREVIOUS_ARCHS" ]]; then
                # Never republish blindly: it could drop architectures we failed to detect.
                echo "WARNING: Could not detect published architectures; falling back to 'publish update'" >&2
            else
                UNION_ARCHS="$(normalize_archs "$PREVIOUS_ARCHS $RECORDED_ARCHS $REQUESTED_ARCHS")"
                if [[ -n "$(comm -23 <(tr ' ' '\n' <<<"$UNION_ARCHS" | sort) <(tr ' ' '\n' <<<"$PREVIOUS_ARCHS" | sort))" ]]; then
                    NEED_REPUBLISH="true"
                fi
            fi
        fi

        if [[ "$NEED_REPUBLISH" == "true" ]]; then
            echo "Republish development repo with architectures: ${UNION_ARCHS// /,}" >&2
            # Record the target set first, so an interrupted run can be recovered.
            echo "$UNION_ARCHS" > "$STATE_FILE"
            aptly publish drop -skip-cleanup "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT"
            DROPPED="true"
            PUBLISHED_OK="false"
            for ATTEMPT in 1 2 3; do
                if publish_dev_repo "${UNION_ARCHS// /,}"; then
                    PUBLISHED_OK="true"
                    break
                fi
                echo "WARNING: Publish attempt $ATTEMPT/3 failed" >&2
                # A failed attempt may leave the endpoint half-created: drop it again.
                if aptly publish list -raw | grep --quiet --line-regexp "$PUBLISH_ENDPOINT $CFG_DISTRO_NAME"; then
                    aptly publish drop -skip-cleanup "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT" || true
                fi
                sleep "${APTLY_PUBLISH_RETRY_SLEEP:-10}"
            done
            if [[ "$PUBLISHED_OK" == "true" ]]; then
                DROPPED="false"
            else
                echo "ERROR: Could not publish with architectures: ${UNION_ARCHS// /,}" >&2
                exit 1  # the EXIT trap restores the previous publication
            fi
        else
            echo "Update already published development repo: $CFG_REPO_NAME" >&2
            aptly publish update -gpg-key="$GPGKEY" -force-overwrite "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT"
            if [[ -n "${PREVIOUS_ARCHS:-}" ]]; then
                echo "$(normalize_archs "$PREVIOUS_ARCHS $RECORDED_ARCHS")" > "$STATE_FILE"
            fi
        fi
    fi
fi

echo "Done!" >&2
