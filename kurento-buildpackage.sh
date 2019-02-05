#!/usr/bin/env bash

#/ Kurento packaging script for Debian/Ubuntu.
#/
#/ This shell script is used to build all Kurento Media Server
#/ modules, and generate Debian/Ubuntu package files from them.
#/
#/ The script must be called from within a Git repository.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --install-kurento <KurentoVersion>
#/
#/   Install Kurento dependencies that are required to build the package.
#/
#/   <KurentoVersion> indicates which Kurento version must be used to download
#/   packages from. E.g.: "6.8.0". If "dev" or "nightly" is given, the
#/   Kurento nightly packages will be used instead.
#/
#/   Typically, you will provide an actual version number when also using
#/   the '--release' flag, and just use "nightly" otherwise. In this mode,
#/   `apt-get` will download and install all required packages from the
#/   Kurento repository for Ubuntu.
#/
#/   If none of the '--install-*' arguments are provided, all required
#/   dependencies are expected to be already installed in the system.
#/
#/   This argument is useful for end users, or external developers which may
#/   want to build a specific component of Kurento without having to build
#/   all the dependencies.
#/
#/   Optional. Default: Disabled.
#/   See also: --install-files
#/
#/ --install-files [FilesDir]
#/
#/   Install Kurento dependencies that are required to build the package.
#/
#/   [FilesDir] is optional, it sets a directory where all '.deb' files
#/   are located with required dependencies.
#/
#/   This argument is useful during incremental builds where dependencies have
#/   been built previously but are still not available to download with
#/   `apt-get`, maybe as a product of previous jobs in a CI pipeline.
#/
#/   If none of the '--install-*' arguments are provided, all required
#/   dependencies are expected to be already installed in the system.
#/
#/   Optional. Default: Disabled.
#/   See also: --install-kurento
#/
#/ --srcdir <SrcDir>
#/
#/   Specifies in which sub-directory the script should work. If not specified,
#/   all operations will be done in the current directory where the script has
#/   been called.
#/
#/   The <SrcDir> MUST contain a 'debian/' directory with all Debian files,
#/   which are used to define how to build the project and generate packages.
#/
#/   This argument is useful for Git projects that contain submodules. Running
#/   directly from a submodule directory might cause some problems if the
#/   command `git-buildpackage` is not able to identify the submodule as a
#/   proper Git repository.
#/
#/   Optional. Default: Current working directory.
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
#/   Allows building packages from a working directory where there are
#/   unstaged and/or uncommited changes.
#/   If this option is not given, the working directory must be clean.
#/
#/   Optional. Default: Disabled.
#/
#/ --release
#/
#/   Build packages intended for Release.
#/   If this option is not given, packages are built as nightly snapshots.
#/
#/   Optional. Default: Disabled.
#/
#/ --timestamp <Timestamp>
#/
#/   Apply the provided timestamp instead of using the date and time this
#/   script is being run.
#/
#/   <Timestamp> must be a decimal number. Ideally, it represents some date
#/   and time when the build was done. It can also be any arbitrary number.
#/
#/   Optional. Default: Current date and time, as given by the command
#/   `date --utc +%Y%m%d%H%M%S`.
#/
#/
#/ Dependency tree
#/ ---------------
#/
#/ * git-buildpackage
#/   - Python 3 (pip, setuptools, wheel)
#/   - debuild (package 'devscripts')
#/     - dpkg-buildpackage (package 'dpkg-dev')
#/     - lintian
#/   - git
#/     - openssh-client (for Git SSH access)
#/ * lsb-release
#/ * mk-build-deps (package 'devscripts')
#/   - equivs
#/ * nproc (package 'coreutils')
#/ * realpath (package 'coreutils')
#/
#/
#/ Dependency install
#/ ------------------
#/
#/ apt-get update && apt-get install --yes \
#/   python3 python3-pip python3-setuptools python3-wheel \
#/   devscripts \
#/   dpkg-dev \
#/   lintian \
#/   git \
#/   openssh-client \
#/   lsb-release \
#/   equivs \
#/   coreutils
#/ pip3 install --upgrade gbp



# ------------ Shell setup ------------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
CONF_FILE="$BASEPATH/kurento.conf.sh"
[[ -f "$CONF_FILE" ]] || {
    echo "[$0] ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}
# shellcheck source=kurento.conf.sh
source "$CONF_FILE"



# ------------ Script start ------------

# Check root permissions
[[ "$(id -u)" -eq 0 ]] || { log "Please run as root"; exit 1; }



# ---- Parse arguments ----

PARAM_INSTALL_KURENTO="false"
PARAM_INSTALL_KURENTO_VERSION="0.0.0"
PARAM_INSTALL_FILES="false"
PARAM_INSTALL_FILES_DIR="$PWD"
PARAM_SRCDIR="$PWD"
PARAM_DSTDIR="$PWD"
PARAM_ALLOW_DIRTY="false"
PARAM_RELEASE="false"
PARAM_TIMESTAMP="$(date --utc +%Y%m%d%H%M%S)"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --install-kurento)
            if [[ -n "${2-}" ]]; then
                PARAM_INSTALL_KURENTO="true"
                PARAM_INSTALL_KURENTO_VERSION="$2"
                shift
            else
                log "ERROR: --install-kurento expects <KurentoVersion>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --install-files)
            PARAM_INSTALL_FILES="true"
            if [[ -n "${2-}" ]]; then
                PARAM_INSTALL_FILES_DIR="$2"
                shift
            fi
            ;;
        --srcdir)
            if [[ -n "${2-}" ]]; then
                PARAM_SRCDIR="$2"
                shift
            else
                log "ERROR: --srcdir expects <SrcDir>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --dstdir)
            if [[ -n "${2-}" ]]; then
                PARAM_DSTDIR="$2"
                shift
            else
                log "ERROR: --dstdir expects <DstDir>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --allow-dirty)
            PARAM_ALLOW_DIRTY="true"
            ;;
        --release)
            PARAM_RELEASE="true"
            ;;
        --timestamp)
            if [[ -n "${2-}" ]]; then
                PARAM_TIMESTAMP="$2"
                shift
            else
                log "ERROR: --timestamp expects <Timestamp>"
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

log "PARAM_INSTALL_KURENTO=${PARAM_INSTALL_KURENTO}"
log "PARAM_INSTALL_KURENTO_VERSION=${PARAM_INSTALL_KURENTO_VERSION}"
log "PARAM_INSTALL_FILES=${PARAM_INSTALL_FILES}"
log "PARAM_INSTALL_FILES_DIR=${PARAM_INSTALL_FILES_DIR}"
log "PARAM_SRCDIR=${PARAM_SRCDIR}"
log "PARAM_DSTDIR=${PARAM_DSTDIR}"
log "PARAM_ALLOW_DIRTY=${PARAM_ALLOW_DIRTY}"
log "PARAM_RELEASE=${PARAM_RELEASE}"
log "PARAM_TIMESTAMP=${PARAM_TIMESTAMP}"



# ---- Internal control variables ----

APT_UPDATE_NEEDED="true"



# ---- Apt configuration ----

# If requested, add the repository
if [[ "$PARAM_INSTALL_KURENTO" == "true" ]]; then
    log "Requested installation of Kurento packages"

    log "Add the Kurento Apt repository key"
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

    if [[ "$PARAM_INSTALL_KURENTO_VERSION" == "nightly" ]]; then
        # Set correct repo name for nightly versions
        REPO="dev"
    else
        REPO="$PARAM_INSTALL_KURENTO_VERSION"
    fi

    log "Add the Kurento Apt repository line"
    APT_FILE="$(mktemp /etc/apt/sources.list.d/kurento-XXXXX.list)"
    DISTRO="$(lsb_release --codename --short)"
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/$REPO $DISTRO kms6" \
        >"$APT_FILE"

    # Adding a new repo requires updating the Apt cache
    if [[ "$APT_UPDATE_NEEDED" == "true" ]]; then
        apt-get update
        APT_UPDATE_NEEDED="false"
    fi
fi

# If requested, install local packages
# This is done _after_ installing from the Kurento repository, because
# installation of local files might be useful to overwrite some default
# version of packages.
if [[ "$PARAM_INSTALL_FILES" == "true" ]]; then
    log "Requested installation of package files"

    FILESDIR="$PARAM_INSTALL_FILES_DIR"

    if ls -f "${FILESDIR}"/*.*deb >/dev/null 2>&1; then
        dpkg --install "${FILESDIR}"/*.*deb || {
            log "Try to install remaining dependencies"
            if [[ "$APT_UPDATE_NEEDED" == "true" ]]; then
                apt-get update
                APT_UPDATE_NEEDED="false"
            fi
            apt-get install --yes --fix-broken --no-remove
        }
    else
        log "No '.deb' package files are present!"
    fi
fi



# ---- Change to Work Directory ----

# All next commands expect to be run from the path that contains
# the actual project and its 'debian/' directory

pushd "$PARAM_SRCDIR" || {
    log "ERROR: Cannot change to source dir: '$PARAM_SRCDIR'"
    exit 1
}



# ---- Dependencies ----

log "Install build dependencies"

if [[ "$APT_UPDATE_NEEDED" == "true" ]]; then
    apt-get update
    APT_UPDATE_NEEDED="false"
fi

mk-build-deps --install --remove \
    --tool='apt-get -o Debug::pkgProblemResolver=yes --no-install-recommends --yes' \
    ./debian/control

# HACK
# By default, 'dh_strip' in Debian will generate '-dbgsym' packages automatically
# from each binary package defined in the control file. This eliminates the need
# to define '-dbg' files explicitly and manually:
#     https://wiki.debian.org/AutomaticDebugPackages
#
# This mechanism also works in Ubuntu 16.04 (Xenial) and earlier, but only if
# the package 'pkg-create-dbgsym' is already installed at build time, so we need
# to install it before building the package.
#
# Ubuntu 18.04 (Bionic) doesn't need this any more, because it already comes
# with Debhelper v10, which has this as the default behavior.
DISTRO_YEAR="$(lsb_release -s -r | cut -d. -f1)"
if [[ $DISTRO_YEAR -lt 18 ]]; then
    apt-get install --yes pkg-create-dbgsym
fi



# ---- Changelog ----

# To build Release packages, the 'debian/changelog' file must be updated and
# committed by a developer, as part of the release process. Then the build
# script uses it to assign a version number to the resulting packages.
# For example, a developer would run:
#     gbp dch --git-author --release debian/
#     git add debian/changelog
#     git commit -m "Update debian/changelog with new release version"
#
# For nightly (pre-release) builds, the 'debian/changelog' file is
# auto-generated by the build script with a snapshot version number. This
# snapshot information is never committed.

# --ignore-branch allows building from a tag or a commit.
#   If it wasn't set, GBP would enforce that the current branch is
#   the "debian-branch" specified in 'gbp.conf' (or 'master' by default).
# --git-author uses the Git user details for the entry in 'debian/changelog'.
if [[ "$PARAM_RELEASE" == "true" ]]; then
    log "Update debian/changelog for a RELEASE version build"
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --release \
        ./debian/
else
    log "Update debian/changelog for a NIGHTLY snapshot build"
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --snapshot --snapshot-number="$PARAM_TIMESTAMP" \
        ./debian/
fi



# ---- Build ----

# --git-ignore-branch allows building from a tag or a commit.
#   If it wasn't set, GBP would enforce that the current branch is
#   the "debian-branch" specified in 'gbp.conf' (or 'master' by default).
# --git-upstream-tree=SLOPPY generates the source tarball from the current
#   state of the working directory.
#   If it wasn't set, GBP would search for upstream source files in
#   the "upstream-branch" specified in 'gbp.conf' (or 'upstream' by default).
# --git-ignore-new ignores the uncommitted 'debian/changelog'.
#
# Other arguments are passed to `debuild` and `dpkg-buildpackage`.

# Arguments passed to 'dpkg-buildpackage'
ARGS="-uc -us -j$(nproc)"
if [[ "$PARAM_ALLOW_DIRTY" == "true" ]]; then
    ARGS="$ARGS -b"
fi

if [[ "$PARAM_RELEASE" == "true" ]]; then
    log "Run git-buildpackage to generate a RELEASE version build"
    gbp buildpackage \
        --git-ignore-branch \
        --git-ignore-new \
        --git-upstream-tree=SLOPPY \
        $ARGS
else
    log "Run git-buildpackage to generate a NIGHTLY snapshot build"
    gbp buildpackage \
        --git-ignore-branch \
        --git-ignore-new \
        --git-upstream-tree=SLOPPY \
        $ARGS
fi



# ---- Move packages and restore previous directory ----

# `dh_builddeb` puts by default the generated '.deb' files in '../'
# so move them to the target destination directory.
# Use 'find | xargs' here because we need to skip moving if the source
# and destination paths are the same.
find "$(realpath ..)" -type f -name '*.*deb' -not -path "$PARAM_DSTDIR/*" -print0 \
    | xargs -0 --no-run-if-empty mv --target-directory="$PARAM_DSTDIR"

popd || true  # Restore dir from "$PARAM_SRCDIR"



log "Done!"
