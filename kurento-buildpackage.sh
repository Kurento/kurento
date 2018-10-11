#!/usr/bin/env bash

#/ Kurento build script.
#/
#/ This shell script is used to build all Kurento Media Server
#/ modules, and generate Debian/Ubuntu package files from them.
#/
#/ Arguments:
#/
#/ --install-missing <Distro> <Version>
#/
#/     Download and install any missing packages from the Kurento packages
#/     repository for Ubuntu.
#/     - <Distro> indicates which Ubuntu version to use.
#/       Must be either "trusty" or "xenial".
#/     - <Version> indicates which Kurento version must be used to download
#/       packages from. E.g.: "6.8.0". If "nightly" is given, then the Kurento
#/       Pre-Release repository will be used.
#/
#/     If this argument is not provided, all required packages are expected to
#/     be already installed in the system.
#/
#/     This option is useful for end users, or external developers which may
#/     want to build a specific component of Kurento without having to build
#/     all the corresponding dependencies.
#/
#/     Optional. Default: False.
#/
#/ --release
#/
#/     Build packages intended for Release.
#/     If this option is not given, packages are built as nightly snapshots.
#/
#/     If none of the '--install-missing' options are given, this build script
#/     expects that all required packages are manually installed beforehand.
#/
#/     Optional. Default: False.
#/
#/ --skip-update
#/
#/     Skip running `apt-get update`.
#/     If you have just updated the Apt cache, you can skip another update
#/     with this argument. It's useful mainly for developers or if you really
#/     know that for some reason you don't want to update.
#/
#/     Optional. Default: False.
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
#/ * mk-build-deps (package 'devscripts')
#/   - equivs
#/ * git-buildpackage (???)
#    2 options:
#    - Ubuntu package: git-buildpackage 0.7.2 in Xenial
#    - Python PIP: gbp 0.9.10
#      - Python 3 (pip, setuptools, wheel)
#      - sudo apt-get install python3-pip python3-setuptools
#      - sudo pip3 install --upgrade gbp
#/   - debuild (package 'devscripts')
#/   - dpkg-buildpackage
#/   - git
#/   - libdistro-info-perl (TODO: why?)
#/   - lintian (TODO: why?)
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
[ "$(id -u)" -eq 0 ] || { echo "Please run as root"; exit 1; }



# ---- Parse arguments ----

PARAM_INSTALL_MISSING=false
PARAM_INSTALL_DISTRO=none
PARAM_INSTALL_VERSION=0.0.0
PARAM_RELEASE=false
PARAM_SKIP_UPDATE=false
PARAM_TIMESTAMP="$(date +%Y%m%d%H%M%S)"

while [[ $# -gt 0 ]]; do
case "${1-}" in
    --install-missing)
        if [[ -n "${2-}" && -n "${3-}" ]]; then
            PARAM_INSTALL_MISSING=true
            PARAM_INSTALL_DISTRO="$2"
            PARAM_INSTALL_VERSION="$3"
            shift
            shift
        else
            log "ERROR: Missing <Distro> <Version>"
            exit 1
        fi
        shift
        ;;
    --release)
        PARAM_RELEASE=true
        shift
        ;;
    --skip-update)
        PARAM_SKIP_UPDATE=true
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

echo "PARAM_INSTALL_MISSING=${PARAM_INSTALL_MISSING}"
echo "PARAM_INSTALL_DISTRO=${PARAM_INSTALL_DISTRO}"
echo "PARAM_INSTALL_VERSION=${PARAM_INSTALL_VERSION}"
echo "PARAM_RELEASE=${PARAM_RELEASE}"
echo "PARAM_SKIP_UPDATE=${PARAM_SKIP_UPDATE}"
echo "PARAM_TIMESTAMP=${PARAM_TIMESTAMP}"



# ---- Apt configuration ----

# If requested, add the repository
if "$PARAM_INSTALL_MISSING"; then
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

    # Set correct repo name for nightly versions
    if [[ "$PARAM_INSTALL_VERSION" = "nightly" ]]; then
        PARAM_INSTALL_VERSION="dev"
    fi

    APT_FILE="$(mktemp /etc/apt/sources.list.d/kurento-XXXXX.list)"
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/$PARAM_INSTALL_VERSION $PARAM_INSTALL_DISTRO kms6" \
        > "$APT_FILE"

    # This requires an Apt cache update
    apt-get update
    PARAM_SKIP_UPDATE=true  # No need to follow through with another update

    # The Apt cache is updated; remove the temporary file
    rm "$APT_FILE"
fi

if ! "$PARAM_SKIP_UPDATE"; then
    apt-get update
fi



# ---- Dependencies ----

mk-build-deps --install --remove \
    --tool='apt-get -o Debug::pkgProblemResolver=yes --no-install-recommends --yes' \
    ./debian/control



# ---- Changelog ----

# To build Release packages, the 'debian/changelog' file must be updated and
# committed a developer, as part of the release process. Then the build script
# uses that information to assign a version number to the resulting packages.
# For example, a developer would run:
#     gbp dch --git-author --release ./debian/
#     git add debian/changelog
#     git commit -m "debian/changelog: New entry for Kurento release 0.1.14"
#
# For nightly (pre-release) builds, the 'debian/changelog' file is
# auto-generated by the build script with a snapshot version number. This
# snapshot information is never committed.

if ! "$PARAM_RELEASE"; then
    # Prepare a nightly snapshot build
    # --ignore-branch allows building from a Git tag.
    #   If not set, GBP would enforce that the current branch is the one
    #   given in 'gbp.conf'.
    # --git-author puts Git user details in 'debian/changelog'.
    gbp dch --ignore-branch --git-author \
        --snapshot --snapshot-number="$PARAM_TIMESTAMP" \
        ./debian/
fi



# ---- Build ----

if "$PARAM_RELEASE"; then
    # --git-ignore-branch allows building from a Git tag.
    #   If not set, GBP would enforce that the current branch is the one
    #   given in 'gbp.conf'.
    # - Other arguments are passed to debuild and dpkg-buildpackage.
    gbp buildpackage --git-ignore-branch \
        -uc -us -j$(nproc)
else
    # --git-ignore-branch allows building from a Git tag.
    #   If not set, GBP would enforce that the current branch is the one
    #   given in 'gbp.conf'.
    # --git-ignore-new ignores the uncommitted debian/changelog from the
    #   Changelog step.
    # --git-upstream-tree=BRANCH generates the source tarball from the branch
    #   given in 'gbp.conf'. TODO: Probably wrong if building from a different branch!
    # - Other arguments are passed to debuild and dpkg-buildpackage.
    gbp buildpackage --git-ignore-branch --git-ignore-new \
        --git-upstream-tree=BRANCH \
        -uc -us -j$(nproc)
fi
