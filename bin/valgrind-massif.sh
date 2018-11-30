#!/usr/bin/env bash

# Launch Massif heap profiler
#
# Massif gathers profiling information, which then can be loaded with `ms_print`
# to present it in a readable way. For example:
#
#     ms_print valgrind-massif-13522.out > valgrind-massif-13522.out.txt
#
# Massif manual: http://valgrind.org/docs/manual/ms-manual.html
#
# Arguments: None.
# Any additional arguments will be passed to the analyzed program.
#
# Dependencies:
# Tested with Valgrind 3.14.0 (Git development branch)
#
# Changes:
# 2014-10-14 Juan Navarro
# - Add header, fix usage of 'nounset'
# 2018-07-06
# - Full rewrite. Add common configuration file with VALGRIND_OPTS.

# ------------ Shell setup ------------

# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trap functions
on_error() { ERROR=1; }
trap on_error ERR
on_exit() { (( ${ERROR-0} )) && echo "[$0] ERROR" || echo "[$0] SUCCESS"; }
trap on_exit EXIT



# ------------ Script start ------------

# Setup Valgrind
if [ -z "$(which valgrind)" ]; then
    # Uncomment one:
    #DIR="/opt/valgrind-3.13.0"
    DIR="/opt/valgrind-git"
    export PATH="${DIR}/bin:${PATH-}"
    export LD_LIBRARY_PATH="${DIR}/lib:${LD_LIBRARY_PATH-}"
    export PKG_CONFIG_PATH="${DIR}/lib/pkgconfig:${PKG_CONFIG_PATH-}"
    export XDG_DATA_DIRS="${DIR}/share:${XDG_DATA_DIRS-}"
    echo "[$0] Configured environment for ${DIR}"
fi

# Load Valgrind configuration
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
CONF_FILE="$BASEPATH/valgrind.conf.sh"
[ -f "$CONF_FILE" ] || {
    echo "[$0] ERROR: Valgrind config not found: $CONF_FILE"
    exit 1
}
source "$CONF_FILE"

# Setup KMS
if [ -z "${GST_DEBUG+x}" ]; then
    # Set GST_DEBUG only if it was previously unset
    #export GST_DEBUG="3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"
    #export GST_DEBUG="3,Kurento*:4,kms*:4"
    export GST_DEBUG="3"
fi

# Start analysis
echo ""
echo "============ Massif heap profiler ============"
echo "Remember to enable the VALGRIND MASSIF configuration in your project"
echo "Best is: RELEASE build with DEBUG symbols, and NORMAL OPTIMIZATION"
echo ""
read -p "Do you want to continue? [Y/n]" -n 1 -r REPLY
echo ""
if [[ "$REPLY" =~ ^[Yy]*$ ]]; then
    echo "[$0] Starting ..."
else
    exit 0;
fi

# Launch target
# Note: "$@" expands to all quoted arguments, as passed to this script
valgrind --tool=massif \
    --log-file=valgrind-massif-%p.log \
    --massif-out-file=valgrind-massif-%p.out \
    "${VALGRIND_OPTIONS[@]}" \
    "$@"
