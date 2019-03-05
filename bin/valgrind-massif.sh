#!/usr/bin/env bash

#/ Massif: a heap profiler.
#/
#/ This scripts launches a program to be analyzed by Valgrind's Massif.
#/ Massif gathers profiling information, which then can be loaded with
#/ the `ms_print` tool to present it in a readable way.
#/
#/ For example:
#/
#/     ms_print valgrind-massif-13522.out >valgrind-massif-13522.out.txt
#/
#/ Massif manual: http://valgrind.org/docs/manual/ms-manual.html
#/
#/
#/ Arguments
#/ ---------
#/
#/ - Path to the program that will be analyzed.
#/ - Any additional arguments will be passed to the analyzed program.
#/
#/
#/ Dependencies
#/ ------------
#/
#/ - Tested with Valgrind 3.14.0 (Git development branch)



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path

# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# shellcheck source=valgrind.conf.sh
source "$BASEPATH/valgrind.conf.sh" || exit 1



# Script start
# ------------

# Trace all commands
set -o xtrace

# Setup Valgrind
if [[ -z "$(which valgrind)" ]]; then
    #DIR="/opt/valgrind-3.13.0"
    DIR="/opt/valgrind-git"
    export PATH+="${PATH:+:}${DIR}/bin"
    export LD_LIBRARY_PATH+="${LD_LIBRARY_PATH:+:}${DIR}/lib"
    export PKG_CONFIG_PATH+="${PKG_CONFIG_PATH:+:}${DIR}/lib/pkgconfig"
    export XDG_DATA_DIRS+="${XDG_DATA_DIRS:+:}${DIR}/share"
    log "Configured environment for ${DIR}"
fi

# Start analysis
log ""
log "==== Massif heap profiler ===="
log "Remember to build your project with VALGRIND MASSIF configuration"
log "Best is: RELEASE build with DEBUG symbols, and NORMAL OPTIMIZATION"
log ""
read -p "Do you want to continue? [Y/n]" -n 1 -r REPLY
[[ "$REPLY" =~ ^[Yy]*$ ]] || exit 0

# Note: "$@" expands to all quoted arguments, as passed to this script
valgrind --tool=massif \
    --log-file="valgrind-massif-%p.log" \
    --massif-out-file="valgrind-massif-%p.out" \
    $VALGRIND_ARGS \
    "$@"
