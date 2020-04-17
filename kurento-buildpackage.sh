#!/usr/bin/env bash

#/ Kurento packaging script for Debian/Ubuntu.
#/
#/ This script is used to build all Kurento Media Server modules, and generate
#/ Debian/Ubuntu package files (.deb) from them. It will automatically install
#/ all required dependencies with `apt-get`, then build the project.
#/
#/ This script must be called from within a Git repository.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --install-kurento <KurentoVersion>
#/
#/   Install dependencies that are required to build the package, using the
#/   Kurento package repository for those packages that need it.
#/
#/   <KurentoVersion> indicates which Kurento repo must be used to download
#/   packages from. E.g.: "6.8.0". If "dev" is given, then Kurento nightly
#/   packages will be used instead.
#/
#/   Typically, you will provide an actual version number when also using the
#/   '--release' flag, and just use "dev" otherwise. With this, `apt-get` will
#/   download and install all required packages from the Kurento repository.
#/
#/   This argument is useful for end users, or external developers which may
#/   want to build a specific component of Kurento without having to build all
#/   the dependencies.
#/
#/   Optional. Default: Disabled.
#/   See also: '--install-files'.
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
#/   See also: '--install-kurento'.
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
#/   command git-buildpackage is not able to identify the submodule as a proper
#/   Git repository.
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
#/   are built as nightly snapshots.
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
#/   equivs \
#/   coreutils
#/ pip3 install --upgrade gbp



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Check permissions
# -----------------

[[ "$(id -u)" -eq 0 ]] || {
    log "ERROR: Please run as root user (or with 'sudo')"
    exit 1
}



# Parse call arguments
# --------------------

CFG_INSTALL_KURENTO="false"
CFG_INSTALL_KURENTO_VERSION="0.0.0"
CFG_INSTALL_FILES="false"
CFG_INSTALL_FILES_DIR="$PWD"
CFG_SRCDIR="$PWD"
CFG_DSTDIR="$PWD"
CFG_ALLOW_DIRTY="false"
CFG_RELEASE="false"
CFG_TIMESTAMP="$(date --utc +%Y%m%d%H%M%S)"

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
                CFG_INSTALL_FILES_DIR="$(realpath $2)"
                shift
            fi
            ;;
        --srcdir)
            if [[ -n "${2-}" ]]; then
                CFG_SRCDIR="$(realpath $2)"
                shift
            else
                log "ERROR: --srcdir expects <SrcDir>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --dstdir)
            if [[ -n "${2-}" ]]; then
                CFG_DSTDIR="$(realpath $2)"
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
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Review config settings
# ----------------------

[[ -d "$CFG_INSTALL_FILES_DIR" ]] || {
    log "ERROR: --install-files given a nonexistent path: '$CFG_INSTALL_FILES_DIR'"
    exit 1
}

[[ -d "$CFG_SRCDIR" ]] || {
    log "ERROR: --srcdir given a nonexistent path: '$CFG_SRCDIR'"
    exit 1
}

[[ -d "$CFG_DSTDIR" ]] || {
    log "ERROR: --dstdir given a nonexistent path: '$CFG_DSTDIR'"
    exit 1
}

log "CFG_INSTALL_KURENTO=$CFG_INSTALL_KURENTO"
log "CFG_INSTALL_KURENTO_VERSION=$CFG_INSTALL_KURENTO_VERSION"
log "CFG_INSTALL_FILES=$CFG_INSTALL_FILES"
log "CFG_INSTALL_FILES_DIR=$CFG_INSTALL_FILES_DIR"
log "CFG_SRCDIR=$CFG_SRCDIR"
log "CFG_DSTDIR=$CFG_DSTDIR"
log "CFG_ALLOW_DIRTY=$CFG_ALLOW_DIRTY"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_TIMESTAMP=$CFG_TIMESTAMP"



# Internal control variables
# --------------------------

APT_UPDATE_NEEDED="true"

# Get Ubuntu version definitions. This brings variables such as:
#
#     DISTRIB_CODENAME="bionic"
#     DISTRIB_RELEASE="18.04"
#
# The file is "/etc/lsb-release" in vanilla Ubuntu installations, but
# "/etc/upstream-release/lsb-release" in Ubuntu-derived distributions
source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release



# Apt configuration
# -----------------

function apt_update_maybe {
    if [[ "$APT_UPDATE_NEEDED" == "true" ]]; then
        apt-get update
        APT_UPDATE_NEEDED="false"
    fi
}

# If requested, add the repository
if [[ "$CFG_INSTALL_KURENTO" == "true" ]]; then
    log "Add the Kurento Apt repository key"
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

    REPO="$CFG_INSTALL_KURENTO_VERSION"

    if grep -qs "ubuntu.openvidu.io/$REPO $DISTRIB_CODENAME kms6" /etc/apt/sources.list.d/kurento.list; then
        log "Kurento Apt repository line already exists"
    else
        log "Kurento Apt repository line has to be added"

        echo "deb [arch=amd64] http://ubuntu.openvidu.io/$REPO $DISTRIB_CODENAME kms6" \
            >>/etc/apt/sources.list.d/kurento.list
    fi

    # Adding a new repo requires updating the Apt cache
    apt_update_maybe
fi

# If requested, install local package files
if [[ "$CFG_INSTALL_FILES" == "true" ]]; then
    log "Install package files from '$CFG_INSTALL_FILES_DIR'"

    if ls -f "$CFG_INSTALL_FILES_DIR"/*.*deb >/dev/null 2>&1; then
        dpkg --install "$CFG_INSTALL_FILES_DIR"/*.*deb || {
            log "Try to install remaining dependencies"
            apt_update_maybe
            apt-get install --yes --fix-broken --no-remove
        }
    else
        log "No '.deb' package files are present!"
    fi
fi



# Enter Work Directory
# --------------------

# All next commands expect to be run from the path that contains
# the actual project and its 'debian/' directory

pushd "$CFG_SRCDIR" || {
    log "ERROR: Cannot change to source dir: '$CFG_SRCDIR'"
    exit 1
}



# Install dependencies
# --------------------

log "Install build dependencies"

apt_update_maybe

# In clean Ubuntu systems 'tzdata' might not be installed yet, but it may be now,
# so make sure interactive prompts from it are disabled
DEBIAN_FRONTEND=noninteractive \
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
#
# REVIEW 2019-02-05 - Disable automatic generation of debug packages
# For now, we'll keep on defining '-dbg' packages in 'debian/control'.
# if [[ ${DISTRIB_RELEASE%%.*} -lt 18 ]]; then
#     apt-get install --yes pkg-create-dbgsym
# fi



# Run git-buildpackage
# --------------------

# To build Release packages, the 'debian/changelog' file must be updated and
# committed by a developer, as part of the release process. Then the build
# script uses it to assign a version number to the resulting packages.
# For example, a developer would run:
#     gbp dch --git-author --release debian/
#     git add debian/changelog
#     git commit -m "Update debian/changelog with new release version"
#
# For nightly (in development) builds, the 'debian/changelog' file is
# auto-generated by the build script with a snapshot version number. This
# snapshot information is never committed.
#
# git-buildpackage arguments:
#
# --git-ignore-new ignores the uncommitted 'debian/changelog'.
#
# --ignore-branch allows building from a tag or a commit.
#   If not set, GBP would enforce that the current branch is the
#   "debian-branch" specified in 'gbp.conf' (or 'master', by default).
#
# --git-upstream-tree=SLOPPY generates the source tarball from the current
#   state of the working directory.
#   If not set, GBP would search for upstream source files in
#   the "upstream-branch" specified in 'gbp.conf' (or 'upstream' by default).
#
# --git-author uses the Git user details for the entry in 'debian/changelog'.
#
# Other arguments are passed to `debuild` and `dpkg-buildpackage`.



# Update debian/changelog
# -----------------------

# A Debian/Ubuntu package repository stores all packages for all components
# and distributions under the same 'pool/' directory. The assumption is that
# two packages with same (name, version, arch) will be exactly the same (MD5).
#
# In our case this is still not true, so we need to differenciate equal packages
# between Ubuntu distributions. For that, the distro version is appended to
# our package version.
#
# This is based on the version scheme used by Firefox packages on Ubuntu:
#
#   Ubuntu Xenial: 65.0+build2-0ubuntu0.16.04.1
#   Ubuntu Bionic: 65.0+build2-0ubuntu0.18.04.1
#
# In which "16.04" or "18.04" is appended to the usual package version.

PACKAGE_VERSION="$(dpkg-parsechangelog --show-field Version)"

if [[ "$CFG_RELEASE" == "true" ]]; then
    log "Update debian/changelog for a RELEASE version build"
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --new-version="${PACKAGE_VERSION}.${DISTRIB_RELEASE}" \
        --release \
        ./debian/
else
    log "Update debian/changelog for a NIGHTLY snapshot build"
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --new-version="${PACKAGE_VERSION}.${DISTRIB_RELEASE}" \
        --snapshot --snapshot-number="$CFG_TIMESTAMP" \
        ./debian/
fi



# Build Debian packages
# ---------------------

GBP_ARGS=""

# `dpkg-buildpackage`: skip signing, use multiple cores
GBP_ARGS="$GBP_ARGS -uc -us -j$(nproc)"

if [[ "$CFG_ALLOW_DIRTY" == "true" ]]; then
    # `dpkg-buildpackage`: build a Binary-only package,
    # skipping `dpkg-source` source tarball altogether
    #GBP_ARGS="$GBP_ARGS -b"

    # `dpkg-source`: generate the source tarball by ignoring
    # ALL changed files in the working directory
    GBP_ARGS="$GBP_ARGS --source-option=--extend-diff-ignore=.*"
elif [[ "$CFG_INSTALL_FILES" == "true" ]]; then
    # `dpkg-source`: generate the source tarball by ignoring
    # '*.deb' and '*.ddeb' files inside $CFG_INSTALL_FILES_DIR
    GBP_ARGS="$GBP_ARGS --source-option=--extend-diff-ignore=.*\.d?deb$"
fi

if [[ "$CFG_RELEASE" == "true" ]]; then
    log "Run git-buildpackage to generate a RELEASE version build"
else
    log "Run git-buildpackage to generate a NIGHTLY snapshot build"
fi

# `debuild`: don't check that the source tarball (".orig.tar.gz") exists;
# this check isn't needed because `git-buildpackage` is just going to create
# the source tarball when it doesn't find it in the working directory.
GBP_BUILDER="debuild --no-tgz-check -i -I"

gbp buildpackage \
    --git-ignore-new \
    --git-ignore-branch \
    --git-upstream-tree=SLOPPY \
    --git-builder="$GBP_BUILDER" \
    $GBP_ARGS



# Move packages
# -------------

# `dh_builddeb` puts by default the generated '.deb' files in '../'
# so move them to the target destination directory.
# Use 'find | xargs' here because we need to skip moving if the source
# and destination paths are the same.
log "Move resulting packages to destination dir: '$CFG_DSTDIR'"
find "$(realpath ..)" -maxdepth 1 -type f -name '*.*deb' \
    -not -path "$CFG_DSTDIR/*" -print0 \
| xargs -0 --no-run-if-empty mv --target-directory="$CFG_DSTDIR"



# Exit Work Directory
# -------------------

popd || true  # "$CFG_SRCDIR"



log "Done!"
