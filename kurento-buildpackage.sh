#!/usr/bin/env bash

#/ Kurento build script.
#/
#/ This shell script is used to build all Kurento Media Server
#/ modules, and generate Debian/Ubuntu package files from them.
#/
#/ Arguments:
#/
#/ --install-missing <Version>
#/
#/     Use `apt-get` to download and install any missing packages from the
#/     Kurento packages repository for Ubuntu.
#/
#/     <Version> indicates which Kurento version must be used to download
#/     packages from. E.g.: "6.8.0". If "dev" or "nightly" is given, the
#/     Kurento Pre-Release package snapshots will be used instead. Typically,
#/     you will provide an actual version number when also using the '--release'
#/     flag, and just use "nightly" otherwise.
#/
#/     If this argument is not provided, all required dependencies are expected
#/     to be already installed in the system.
#/
#/     This option is useful for end users, or external developers which may
#/     want to build a specific component of Kurento without having to build
#/     all the corresponding dependencies.
#/
#/     Optional. Default: Disabled.
#/
#/ --release
#/
#/     Build packages intended for Release.
#/     If this option is not given, packages are built as nightly snapshots.
#/
#/     If none of the '--install-missing' options are given, this build script
#/     expects that all required packages are manually installed beforehand.
#/
#/     Optional. Default: Disabled.
#/
#/ --timestamp <Timestamp>
#/
#/    Apply the provided timestamp instead of using the date and time this
#/    script is being run.
#/    <Timestamp> can be in any format accepted by the `date` command,
#/    for example in ISO 8601 format: "2018-12-31T23:58:59".
#/
#/     Optional. Default: Current timestamp, as given by `date +%Y%m%d%H%M%S`.
#/
#/ Dependency tree:
#/
#/ * git-buildpackage (???)
#    2 options:
#    - Ubuntu package: git-buildpackage 0.7.2 in Xenial
#    - Python PIP: gbp 0.9.10
#      - Python 3 (pip, setuptools, wheel)
#      - sudo apt-get install python3-pip python3-setuptools
#      - sudo pip3 install --upgrade gbp
#/   - debuild (package 'devscripts')
#/     - dpkg-buildpackage (package 'dpkg-dev')
#/     - lintian
#/   - git
#/ * lsb-release
#/ * mk-build-deps (package 'devscripts')
#/   - equivs
#/ * nproc (package 'coreutils')



# ------------ Shell setup ------------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
CONF_FILE="$BASEPATH/kurento.conf.sh"
[ -f "$CONF_FILE" ] || {
    echo "[$0] ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}
# shellcheck source=kurento.conf.sh
source "$CONF_FILE"



# ------------ Script start ------------

# Check root permissions
[ "$(id -u)" -eq 0 ] || { log "Please run as root"; exit 1; }



# ---- Parse arguments ----

PARAM_INSTALL_MISSING=false
PARAM_INSTALL_VERSION=0.0.0
PARAM_RELEASE=false
PARAM_TIMESTAMP="$(date +%Y%m%d%H%M%S)"

while [[ $# -gt 0 ]]; do
case "${1-}" in
    --install-missing)
        if [[ -n "${2-}" ]]; then
            PARAM_INSTALL_MISSING=true
            PARAM_INSTALL_VERSION="$2"
            shift
        else
            log "ERROR: Missing <Version>"
            exit 1
        fi
        shift
        ;;
    --release)
        PARAM_RELEASE=true
        shift
        ;;
    --timestamp)
        if [[ -n "${2-}" ]]; then
            PARAM_TIMESTAMP="$(date --date="$2" +%Y%m%d%H%M%S)"
            shift
        else
            log "ERROR: Missing <Timestamp>"
            exit 1
        fi
        shift
        ;;
    *)
        log "ERROR: Unknown argument '${1-}'"
        exit 1
        ;;
esac
done

log "PARAM_INSTALL_MISSING=${PARAM_INSTALL_MISSING}"
log "PARAM_INSTALL_VERSION=${PARAM_INSTALL_VERSION}"
log "PARAM_RELEASE=${PARAM_RELEASE}"
log "PARAM_TIMESTAMP=${PARAM_TIMESTAMP}"



# ---- Apt configuration ----

# If requested, add the repository
if "$PARAM_INSTALL_MISSING"; then
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

    # Set correct repo name for nightly versions
    if [[ "$PARAM_INSTALL_VERSION" = "nightly" ]]; then
        PARAM_INSTALL_VERSION="dev"
    fi

    APT_FILE="$(mktemp /etc/apt/sources.list.d/kurento-XXXXX.list)"
    DISTRO="$(lsb_release --short --codename)"
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/$PARAM_INSTALL_VERSION $DISTRO kms6" \
        >"$APT_FILE"

    # This requires an Apt cache update
    apt-get update
fi



# ---- Dependencies ----

mk-build-deps --install --remove \
    --tool='apt-get -o Debug::pkgProblemResolver=yes --no-install-recommends --yes' \
    ./debian/control



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
if "$PARAM_RELEASE"; then
    # Prepare a release version build
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --release \
        debian/
else
    # Prepare a nightly snapshot build
    gbp dch \
        --ignore-branch \
        --git-author \
        --spawn-editor=never \
        --snapshot --snapshot-number="$PARAM_TIMESTAMP" \
        debian/
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
# - Other arguments are passed to debuild and dpkg-buildpackage.
if "$PARAM_RELEASE"; then
    gbp buildpackage \
        --git-ignore-branch \
        --git-ignore-new \
        --git-upstream-tree=SLOPPY \
        -uc -us -j$(nproc)
else
    gbp buildpackage \
        --git-ignore-branch \
        --git-ignore-new \
        --git-upstream-tree=SLOPPY \
        -uc -us -j$(nproc)
fi



# ---- Results ----

log "Files:"
ls -1 ../*.*
cp ../*.*deb ./
