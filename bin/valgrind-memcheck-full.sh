#!/usr/bin/env bash

# Launch Memcheck memory error detector (with slow options)
#
# Arguments: (none)
#
# Dependencies:
# Valgrind 3.10.0 (Ubuntu 14.04)
#
# Changes:
# 2014-10-14 Juan Navarro
# - Add header, fix usage of 'nounset'

# ------------ Shell setup ------------

# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trap functions
on_error() { ERROR=1; }
trap on_error ERR
on_exit() { (( ${ERROR:-0} )) && echo "[$0] ERROR" || echo "[$0] SUCCESS"; }
trap on_exit EXIT



# ------------ Script start ------------

# Setup Valgrind
if [ -z "$(which valgrind)" ]; then
    # Uncomment one:
    #NAME="valgrind-3.13.0"
    NAME="valgrind-git"
    export PATH="/opt/${NAME}/bin:${PATH:-}"
    export LD_LIBRARY_PATH="/opt/${NAME}/lib:${LD_LIBRARY_PATH:-}"
    export PKG_CONFIG_PATH="/opt/${NAME}/lib/pkgconfig:${PKG_CONFIG_PATH:-}"
    export XDG_DATA_DIRS="/opt/${NAME}/share:${XDG_DATA_DIRS:-}"
fi

# Setup KMS
#export GST_DEBUG="3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"
#export GST_DEBUG="3,Kurento*:4,kms*:4"
export GST_DEBUG="3"

# Start analysis
echo ""
echo "============ Memcheck memory error detector (slow options) ============"
echo "Remember to enable the VALGRIND MEMCHECK configuration in your project"
echo "Best is: RELEASE build with DEBUG symbols, and MINIMAL OPTIMIZATION"
echo ""
read -p "Do you want to continue? [Y/n]" -n 1 -r REPLY
echo ""
if [[ "$REPLY" =~ ^[Yy]*$ ]]; then
    echo "Starting ..."
else
    exit 0;
fi

# Valgrind common options:
#   --verbose
#   --trace-children=<yes|no> [default: no]
#     Trace into sub-processes initiated via the exec system call.
#   --vgdb=no
#   --log-file=<FileNameWithPatterns>
#   --error-limit=no
#   --suppressions=<SuppressionsFile1.supp>
#   --suppressions=<SuppressionsFile2.supp>
#   --smc-check=all-non-file
#   --read-var-info=<yes|no> [default: no]
#     Slows Valgrind startup significantly and makes it use significantly more
#     memory, but for Memcheck, Helgrind, DRD, it can result in more precise
#     error messages.
#   --fair-sched=<yes|no|try> [default: no]
#     Controls the way the threads are scheduled. The fairness of the
#     futex-based locking produces better reproducibility of thread scheduling
#     for different executions of a multithreaded application. This better
#     reproducibility is particularly helpful when using Helgrind or DRD.
#
# Memcheck options:
#     TODO
# --show-possibly-lost=yes
# --leak-check-heuristics=all

# Launch target
# Note: '$*' expands to this script's arguments

BUILDDIR="build-Release"
[ -d "$BUILDDIR" ] || {
    echo "Release build not found: $PWD/$BUILDDIR/"
    exit 1
}
WORKDIR="$(cd -P "$BUILDDIR" && pwd -P)"  # Absolute canonical path

valgrind \
    --tool=memcheck \
    --verbose \
    --log-file=kms-memcheck-full-%p.log \
    --trace-children=yes \
    --vgdb=no \
    --error-limit=no \
    --suppressions="$PWD/3rdparty/valgrind/GNOME.supp" \
    --suppressions="$PWD/3rdparty/valgrind/debian.supp" \
    --suppressions="$PWD/3rdparty/valgrind/glib.supp" \
    --suppressions="$PWD/3rdparty/valgrind/gst.supp" \
    --suppressions="$PWD/3rdparty/valgrind/walbottle.supp" \
    --smc-check=all-non-file \
    --read-var-info=yes \
    --fair-sched=yes \
    --leak-check=full \
    --show-leak-kinds=definite,indirect \
    --track-origins=yes \
    --partial-loads-ok=yes \
    --keep-stacktraces=alloc-and-free \
    --show-possibly-lost=yes \
    --leak-check-heuristics=all \
    "$WORKDIR/kurento-media-server/server/kurento-media-server" \
      --modules-path="$WORKDIR" \
      --modules-config-path="$WORKDIR/config" \
      --conf-file="$WORKDIR/config/kurento.conf.ini" \
      --gst-plugin-path="$WORKDIR" \
      $*
