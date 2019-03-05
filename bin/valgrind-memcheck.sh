#!/usr/bin/env bash

#/ Memcheck: a memory error detector.
#/
#/ This scripts launches a program to be analyzed by Valgrind's Memcheck.
#/
#/ Memcheck manual: http://valgrind.org/docs/manual/mc-manual.html
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
log "==== Memcheck memory error detector ===="
log "Remember to build your project with VALGRIND MEMCHECK configuration"
log "Best is: RELEASE build with DEBUG symbols, and MINIMAL OPTIMIZATION"
log ""
read -p "Do you want to continue? [Y/n]" -n 1 -r REPLY
[[ "$REPLY" =~ ^[Yy]*$ ]] || exit 0

# Note: "$@" expands to all quoted arguments, as passed to this script
valgrind --tool=memcheck \
    --log-file="valgrind-memcheck-%p.log" \
    $VALGRIND_ARGS \
    "$@"
