#!/usr/bin/env bash

#/ Kurento build script.
#/
#/ This shell script is used to build all Kurento Media Server
#/ modules, and generate Debian/Ubuntu package files from them.
#/
#/ Arguments:
#/
#/ -r | --release
#/
#/     Build packages intended for Release.
#/     If this option is not given, packages are built as nightly snapshots.
#/
#/ -i | --install-missing
#/
#/     Download and install any missing packages from Kurento repositories.
#/     If this option is not given, the build script expects that all required
#/     packages will have been installed beforehand.
#/
#/ -u | --skip-update
#/
#/     Skip running `apt-get update`.
#/     If you have just updated the Apt cache, this option allows to avoid
#/     updating again. It's useful mainly for developers or if you really know
#/     you don't want to update.
#/
#/ -t <Timestamp> | --timestamp <Timestamp>
#/
#/    Apply the provided timestamp instead of using the date and time this
#/    script is being run.
#/    The input <Timestamp> can be in any format accepted by the `date` command,
#/    for example in ISO 8601 format: "2018-12-31T23:58:59".



# ------------ Shell setup ------------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
CONF_FILE="$BASEPATH/kurento.conf.sh"
[ -f "$CONF_FILE" ] || {
    echo "[$0] ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}
source "$CONF_FILE"



# ------------ Script start ------------

PARAM_RELEASE=false
PARAM_INSTALL=false
PARAM_UPDATE=true
PARAM_TIMESTAMP="$(date +%Y%m%d%H%M%S)"

while [[ $# -gt 0 ]]; do
case $1 in
    -r|--release)
        PARAM_RELEASE=true
        shift
        ;;
    -i|--install-missing)
        PARAM_INSTALL=true
        shift
        ;;
    -u|--skip-update)
        PARAM_UPDATE=true
        shift
        ;;
    -t|--timestamp)
        [[ -n "${2-}" ]] && {
            PARAM_TIMESTAMP="$(date --date="$2" +%Y%m%d%H%M%S)"
            shift
        }
        shift
        ;;
    * )
        usage
        ;;
esac
done

echo "PARAM_RELEASE=${PARAM_RELEASE}"
echo "PARAM_INSTALL=${PARAM_INSTALL}"
echo "PARAM_UPDATE=${PARAM_UPDATE}"
echo "PARAM_TIMESTAMP=${PARAM_TIMESTAMP}"
