#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Kurento packaging script for Debian/Ubuntu.
#/
#/ This script is used to build all Kurento Media Server modules, and generate
#/ Debian/Ubuntu package files (.deb) from them. It will automatically install
#/ all required dependencies with `apt-get`, then build the project.
#/
#/ If running on a Git repository, information about the current commit will be
#/ added to the resulting version number (for development builds).
#/
#/
#/
#/ Arguments
#/ ---------
#/
#/ --install-kurento <KurentoVersion>
#/
#/   Install dependencies that are required to build the package, using the
#/   Kurento package repository for those packages that need it. This is useful
#/   for quickly building a specific component of Kurento (e.g. "kurento-module-core")
#/   without also having to build all of its dependencies.
#/
#/   <KurentoVersion> indicates which Kurento repo should be used to download
#/   packages from. E.g.: "7.0.0", or "dev" for development builds. Typically,
#/   you will provide an actual version number when also using '--release', and
#/   just use "dev" otherwise.
#/
#/   The appropriate Kurento repository line for apt-get must be already present
#/   in some ".list" file under /etc/apt/. To have this script adding the
#/   required line automatically for you, use '--apt-add-repo'.
#/
#/   Optional. Default: Disabled.
#/   See also:
#/     --install-files
#/     --apt-add-repo
#/
#/ --install-files [FilesDir]
#/
#/   Install specific dependency files that are required to build the package.
#/
#/   [FilesDir] is optional, and defaults to the current working directory. It
#/   tells this tool where all '.deb' files are located, to be installed.
#/
#/   This argument is useful during incremental builds where dependencies have
#/   been built previously but are still not available to download with
#/   `apt-get`, maybe as a product of previous jobs in a CI pipeline.
#/
#/   '--install-files' can be used together with '--install-kurento'. If none of
#/   the '--install-*' arguments are provided, all non-system dependencies are
#/   expected to be already installed.
#/
#/   Optional. Default: Disabled.
#/   See also:
#/     --install-kurento
#/
#/ --dstdir <DstDir>
#/
#/   Specifies where the resulting Debian package files ('*.deb') should be
#/   placed after the build finishes.
#/
#/   Optional. Default: Current working directory.
#/
#/ --allow-dirty
#/
#/   Allows building packages from a working directory where there are unstaged
#/   and/or uncommited source code changes. If this option is not given, the
#/   working directory must be clean.
#/
#/   NOTE: This tells `dpkg-buildpackage` to skip calling `dpkg-source` and
#/   build a Binary-only package. It makes easier creating a test package, but
#/   in the long run the objective is to create oficially valid packages which
#/   comply with Debian/Ubuntu's policies, so this option should not be used for
#/   final release builds.
#/
#/   Optional. Default: Disabled.
#/
#/ --release
#/
#/   Build packages intended for Release. If this option is not given, packages
#/   are built as development snapshots.
#/
#/   Optional. Default: Disabled.
#/
#/ --timestamp <Timestamp>
#/
#/   Apply the provided timestamp instead of using the date and time this script
#/   is being run.
#/
#/   <Timestamp> must be a decimal number. Ideally, it represents some date and
#/   time when the build was done. It can also be any arbitrary number.
#/
#/   Optional. Default: Current date and time, as given by the command
#/   `date --utc +%Y%m%d%H%M%S`.
#/
#/ --apt-add-repo
#/
#/   Edit the system config to add a Kurento repository line for apt-get.
#/
#/   This adds or edits the file "/etc/apt/sources.list.d/kurento.list" to make
#/   sure that `apt-get` will be able to download and install all required
#/   packages from the Kurento repository, if the line wasn't already there.
#/
#/   To use this argument, '--install-kurento' must be used too.
#/
#/ --apt-proxy <ProxyUrl>
#/
#/   Use the given HTTP proxy for apt-get. This can be useful in environments
#/   where such a proxy is set up, in order to save on data transfer costs from
#/   official system repositories.
#/
#/   <ProxyUrl> is set to Apt option "Acquire::http::Proxy".
#/
#/   Doc: https://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html
#/
#/
#/
#/ Dependency tree
#/ ---------------
#/
#/ * git-buildpackage
#/   - debuild (package 'devscripts')
#/     - dpkg-buildpackage (package 'dpkg-dev')
#/     - lintian
#/   - git
#/     - openssh-client (for Git SSH access)
#/ * mk-build-deps (package 'devscripts')
#/   - equivs
#/ * nproc (package 'coreutils')
#/ * realpath (package 'coreutils')



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Check permissions
# =================

[[ "$(id -u)" -eq 0 ]] || {
    log "ERROR: Please run as root user (or with 'sudo')"
    exit 1
}



# Parse call arguments
# ====================

CFG_INSTALL_KURENTO="false"
CFG_INSTALL_KURENTO_VERSION=""
CFG_INSTALL_FILES="false"
CFG_INSTALL_FILES_DIR="$PWD"
CFG_DSTDIR="$PWD"
CFG_ALLOW_DIRTY="false"
CFG_RELEASE="false"
CFG_TIMESTAMP="$(date --utc +%Y%m%d%H%M%S)"
CFG_APT_ADD_REPO="false"
CFG_APT_PROXY_URL=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --install-kurento)
            if [[ -n "${2-}" ]]; then
                CFG_INSTALL_KURENTO="true"
                CFG_INSTALL_KURENTO_VERSION="$2"
                shift
            else
                log "ERROR: --install-kurento expects <KurentoVersion>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --install-files)
            CFG_INSTALL_FILES="true"
            if [[ -n "${2-}" ]]; then
                CFG_INSTALL_FILES_DIR="$(realpath "$2")"
                shift
            fi
            ;;
        --dstdir)
            if [[ -n "${2-}" ]]; then
                CFG_DSTDIR="$(realpath "$2")"
                shift
            else
                log "ERROR: --dstdir expects <DstDir>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --allow-dirty)
            CFG_ALLOW_DIRTY="true"
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        --timestamp)
            if [[ -n "${2-}" ]]; then
                CFG_TIMESTAMP="$2"
                shift
            else
                log "ERROR: --timestamp expects <Timestamp>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --apt-add-repo)
            CFG_APT_ADD_REPO="true"
            ;;
        --apt-proxy)
            if [[ -n "${2-}" ]]; then
                CFG_APT_PROXY_URL="$2"
                shift
            else
                log "ERROR: --apt-proxy expects <ProxyUrl>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

[[ -d "$CFG_INSTALL_FILES_DIR" ]] || {
    log "ERROR: --install-files given a nonexistent path: '$CFG_INSTALL_FILES_DIR'"
    exit 1
}

[[ -d "$CFG_DSTDIR" ]] || {
    log "ERROR: --dstdir given a nonexistent path: '$CFG_DSTDIR'"
    exit 1
}

[[ "$CFG_INSTALL_KURENTO" == "true" ]] || {
    CFG_APT_ADD_REPO="false"
}

log "CFG_INSTALL_KURENTO=$CFG_INSTALL_KURENTO"
log "CFG_INSTALL_KURENTO_VERSION=$CFG_INSTALL_KURENTO_VERSION"
log "CFG_INSTALL_FILES=$CFG_INSTALL_FILES"
log "CFG_INSTALL_FILES_DIR=$CFG_INSTALL_FILES_DIR"
log "CFG_DSTDIR=$CFG_DSTDIR"
log "CFG_ALLOW_DIRTY=$CFG_ALLOW_DIRTY"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_TIMESTAMP=$CFG_TIMESTAMP"
log "CFG_APT_ADD_REPO=$CFG_APT_ADD_REPO"
log "CFG_APT_PROXY_URL=$CFG_APT_PROXY_URL"



# Configure apt-get
# =================

# Get Ubuntu version definitions. This brings variables such as:
#     DISTRIB_CODENAME="focal"
#     DISTRIB_RELEASE="20.04"
source /etc/lsb-release

# Extra options for all apt-get invocations
APT_ARGS=()

# If requested, use an Apt proxy
if [[ -n "$CFG_APT_PROXY_URL" ]]; then
    APT_ARGS+=("-o")
    APT_ARGS+=("Acquire::http::Proxy=$CFG_APT_PROXY_URL")
fi

# If requested to install Kurento packages, verify the Apt repos
if [[ "$CFG_INSTALL_KURENTO" == "true" ]]; then

    REPO="$CFG_INSTALL_KURENTO_VERSION"
    LIST_LINE="deb [arch=amd64] http://ubuntu.openvidu.io/$REPO $DISTRIB_CODENAME main"

    if LIST_FILE="$(grep --recursive --files-with-matches --include='*.list' "$LIST_LINE" /etc/apt/)"; then
        log "Found Kurento repository line for apt-get:"
        log "$LIST_FILE"
    else
        # If requested, add the repository
        if [[ "$CFG_APT_ADD_REPO" == "true" ]]; then
            log "Add Kurento repository key for apt-get"
            apt-key adv \
                --keyserver hkp://keyserver.ubuntu.com:80 \
                --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83

            log "Add Kurento repository line for apt-get"
            echo "$LIST_LINE" | tee -a /etc/apt/sources.list.d/kurento.list
        else
            log "ERROR: Could not find Kurento repository line for apt-get"
            log ""
            log "Suggested solution 1:"
            log "    Re-run with '--apt-add-repo'"
            log ""
            log "Suggested solution 2:"
            log "    Run commands:"
            log "    $ apt-key adv \\"
            log "        --keyserver hkp://keyserver.ubuntu.com:80 \\"
            log "        --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83"
            log "    $ echo '$LIST_LINE' | sudo tee -a /etc/apt/sources.list.d/kurento.list"
            log ""
            exit 1
        fi
    fi
fi

# If requested, install local package files
if [[ "$CFG_INSTALL_FILES" == "true" ]]; then
    log "Install package files from '$CFG_INSTALL_FILES_DIR'"

    if ls -f $(find "$CFG_INSTALL_FILES_DIR" -name '*.*deb') >/dev/null 2>&1; then
        dpkg --install $(find "$CFG_INSTALL_FILES_DIR" -name '*.*deb') || {
            log "Try to install remaining dependencies"
            apt-get update ; apt-get "${APT_ARGS[@]}" install --fix-broken --no-remove --yes
        }
    else
        log "No '.deb' package files are present!"
    fi
fi



# Install dependencies
# ====================

log "Install build dependencies"

# Notes:
# * DEBIAN_FRONTEND: In clean Ubuntu systems 'tzdata' might not be installed
#   yet, but it may be now, so make sure interactive dialogues are disabled.
# * Debug::pkgProblemResolver=yes: Show details about the dependency resolution.
#   Doc: http://manpages.ubuntu.com/manpages/man5/apt.conf.5.html
# * --target-release '*-backports': Prefer installing newer versions of packages
#   from the backports repository.
(
    export DEBIAN_FRONTEND=noninteractive

    apt-get update ; mk-build-deps --install --remove \
        --tool="apt-get ${APT_ARGS[*]} -o Debug::pkgProblemResolver=yes --target-release 'a=$DISTRIB_CODENAME-backports' --no-install-recommends --no-remove --yes" \
        ./debian/control
)



# Update changelog
# ================

# To build Release packages, the 'debian/changelog' file must be updated and
# committed by a developer, as part of the release process. Then the build
# script uses it to assign a version number to the resulting packages.

# For development builds, the 'debian/changelog' file is auto-generated by the
# build script with a snapshot version number. This information isn't committed.

(
    # A Debian/Ubuntu package repository stores all packages for all components
    # and distributions under the same 'pool/' directory. The assumption is that
    # two packages with same (name, version, arch) will be exactly the same (MD5).
    #
    # In our case this is still not true, so we need to differentiate equal
    # packages between Ubuntu distributions. For that, the distro version is
    # appended to our package version.
    #
    # This is based on the version scheme used by Firefox packages on Ubuntu,
    # where the release number (e.g. 16.04 or 18.04) is appended to the version.
    #
    #   Ubuntu Xenial: 65.0+build2-0ubuntu0.16.04.1
    #   Ubuntu Bionic: 65.0+build2-0ubuntu0.18.04.1
    #
    PACKAGE_VERSION="$(dpkg-parsechangelog --show-field Version)"
    DCH_VERSION="$PACKAGE_VERSION.$DISTRIB_RELEASE"

    # debchange (dch) requires an email being set on the system.
    if [[ -z "${DEBFULLNAME:-}${NAME:-}" || -z "${DEBEMAIL:-}${EMAIL:-}" ]]; then
        DEBFULLNAME="$(git config --default 'Kurento' --get user.name)"; export DEBFULLNAME
        DEBEMAIL="$(git config --default 'kurento@openvidu.io' --get user.email)"; export DEBEMAIL
    fi

    if [[ "$CFG_RELEASE" == "true" ]]; then
        log "Update debian/changelog for a RELEASE version build"

        # Add the release message to the currently opened changelog entry.
        dch "Prepare release $PACKAGE_VERSION"

        # Close the current changelog entry and mark it as released.
        dch \
            --release \
            --distribution "testing" --force-distribution \
            ""
    else
        log "Update debian/changelog for a DEVELOPMENT snapshot build"

        SNAPSHOT_TIME="$CFG_TIMESTAMP"

        # If running within a Git repo, also append the commit hash.
        # Otherwise, this info will just be omitted.
        SNAPSHOT_HASH="$(git rev-parse --short HEAD 2>/dev/null || true)"

        dch \
            --newversion "$DCH_VERSION~$SNAPSHOT_TIME${SNAPSHOT_HASH:+".git$SNAPSHOT_HASH"}" \
            ""
    fi
)



# Build packages
# ==============

BUILD_CMD_ARGS=()

BUILD_CMD_ARGS+=(
    # (-b) Build a binary-only package, skipping `dpkg-source`.
    "--build=binary"

    # (-uc) Do not sign the .buildinfo and .changes files.
    "--unsigned-changes"

    # (-us) Do not sign the source package.
    "--unsigned-source"
)

# Debhelper and all dpkg-related tools: Parallelize build jobs
# This can be overriden with DEB_BUILD_OPTIONS. For example:
#     $ DEB_BUILD_OPTIONS="parallel=2" ./kurento-buildpackage.sh
if [[ ! "${DEB_BUILD_OPTIONS:-}" =~ "parallel" ]]; then
    BUILD_CMD_ARGS+=("-j$(nproc)")
fi

if [[ "$CFG_ALLOW_DIRTY" == "true" ]]; then
    BUILD_CMD_ARGS+=(
        # Generate the source tarball by ignoring ALL changed files
        # in the working directory.
        "--source-option=--extend-diff-ignore=.*"
    )
elif [[ "$CFG_INSTALL_FILES" == "true" ]]; then
    BUILD_CMD_ARGS+=(
        # Generate the source tarball by ignoring '*.deb' and '*.ddeb' files
        # inside $CFG_INSTALL_FILES_DIR.
        "--source-option=--extend-diff-ignore=.*\.d?deb$"
    )
fi

if [[ "$CFG_RELEASE" == "true" ]]; then
    log "Running package builder for a RELEASE version"
else
    log "Running package builder for a DEVELOPMENT snapshot"
fi

# Print logs from tests
export BOOST_TEST_LOG_LEVEL=test_suite
export CTEST_OUTPUT_ON_FAILURE=1

# GStreamer: Don't log with colors (avoid ANSI escape codes in test output)
export GST_DEBUG_NO_COLOR=1

dpkg-buildpackage "${BUILD_CMD_ARGS[@]}"



# Move packages
# =============

# `dh_builddeb` puts by default the generated '.deb' files in '../'
# so move them to the target destination directory.
# Use 'find | xargs' here because we need to skip moving if the source
# and destination paths are the same.
log "Move resulting packages to destination dir: '$CFG_DSTDIR'"
find "$(realpath ..)" -maxdepth 1 -type f -name '*.*deb' \
    -not -path "$CFG_DSTDIR/*" -print0 \
| xargs -0 --no-run-if-empty mv --target-directory="$CFG_DSTDIR"
