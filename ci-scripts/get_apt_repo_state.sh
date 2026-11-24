#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Print a digest of the current state of a Kurento Apt repository.
#/
#/ The digest is the SHA-256 of the `Release` file of the repository, which
#/ changes every time new packages are published. It is meant to be used as a
#/ Docker build argument, to invalidate the cached layers that install Kurento
#/ packages whenever the repository contents change.
#/
#/ Usage:
#/
#/     APT_STATE="$(get_apt_repo_state.sh <RepoName> <Distro>)"
#/
#/ * <RepoName>: Name of the repository, e.g. "dev", "dev-pr-123", "7.3.1".
#/ * <Distro>: Ubuntu codename, e.g. "noble".
#/
#/ If the repository cannot be queried, a unique value is printed instead, so
#/ the Docker cache is bypassed. A stale image is worse than a slow build.
#/
#/ Environment variables:
#/
#/ APT_REPO_URL
#/
#/   Base URL of the Apt server. Optional. Default: "http://ubuntu.openvidu.io".



# WARNING: DO NOT PRINT LOGGING MESSAGES TO STDOUT.
# Callers of this script expect that the output only contains the digest.
# If you need debug, make sure it gets redirected to stderr (`command >&2`).

set -o errexit -o errtrace -o pipefail -o nounset

REPO_NAME="${1:?Usage: get_apt_repo_state.sh <RepoName> <Distro>}"
DISTRO="${2:?Usage: get_apt_repo_state.sh <RepoName> <Distro>}"
APT_REPO_URL="${APT_REPO_URL:-http://ubuntu.openvidu.io}"

URL="$APT_REPO_URL/$REPO_NAME/dists/$DISTRO/Release"

# Use curl if available, otherwise wget (the only one required on the runners).
fetch() {
    if command -v curl >/dev/null; then
        curl --fail --silent --show-error --location --connect-timeout 10 \
            --retry 3 --retry-delay 2 --max-time 30 "$1"
    else
        wget --quiet --output-document=- --connect-timeout=10 --timeout=30 \
            --tries=4 --waitretry=2 "$1"
    fi
}

if STATE="$(
    fetch "$URL" \
    | sha256sum | cut --delimiter=' ' --fields=1
)" && [[ "$STATE" =~ ^[0-9a-f]{64}$ ]] \
&& [[ "$STATE" != "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" ]]; then
    echo "$STATE"
else
    echo "WARNING: Cannot read '$URL'; the Docker cache will be bypassed" >&2
    date --utc +nocache-%Y%m%d%H%M%S%N
fi
