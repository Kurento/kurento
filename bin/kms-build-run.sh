#!/usr/bin/env bash

#/ Build and run Kurento Media Server.
#/
#/ This shell script builds KMS, and/or runs it with default options if
#/ already built.
#/
#/ To use, first clone the KMS omni-build repo and its submodules:
#/
#/     git clone https://github.com/Kurento/kms-omni-build.git
#/     cd kms-omni-build/
#/     git submodule update --init --recursive
#/     git submodule update --remote
#/
#/ Then run this script directly from that directory:
#/
#/     bin/kms-build-run.sh
#/
#/
#/ Arguments
#/ ---------
#/
#/ --build-only
#/
#/   Only build the source code, without actually running KMS.
#/
#/   Optional. Default: Disabled.
#/
#/ --release
#/
#/   Build in Release mode with debugging symbols.
#/
#/   If this option is not given, CMake will be configured for a Debug build.
#/
#/   Optional. Default: Disabled.
#/
#/ --gdb
#/
#/   Run KMS in a GDB session. Useful to set break points and get backtraces.
#/
#/   Optional. Default: Disabled.
#/
#/ --clang
#/
#/   Build (and run, in case of using a Sanitizer) with Clang C/C++ compiler.
#/
#/   Note: You are still in charge of providing the desired version of Clang in
#/   `/usr/bin/clang` for C; `/usr/bin/clang++` for C++.
#/   For this, either create symlinks manually, or have a look into the
#/   Debian/Ubuntu alternatives system (`update-alternatives`).
#/
#/   Optional. Default: Disabled. When disabled, the compiler will be GCC.
#/
#/ --verbose
#/
#/   Tell CMake to generate verbose Makefiles, that will print every build
#/   command as they get executed by `make`.
#/
#/   Optional. Default: Disabled.
#/
#/ --valgrind-memcheck
#/
#/   Build and run with Valgrind's Memcheck memory error detector.
#/   Valgrind should be available in the PATH.
#/
#/   See:
#/   * Memcheck manual: http://valgrind.org/docs/manual/mc-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.
#/
#/ --valgrind-massif
#/
#/   Build and run with Valgrind's Massif heap profiler.
#/   Valgrind should be available in the PATH.
#/
#/   Massif gathers profiling information, which then can be loaded with
#/   the `ms_print` tool to present it in a readable way.
#/
#/   For example:
#/
#/       ms_print valgrind-massif-13522.out >valgrind-massif-13522.out.txt
#/
#/   See:
#/   * Massif manual: http://valgrind.org/docs/manual/ms-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.
#/
#/ --valgrind-callgrind
#/
#/   Build and run with Valgrind's Callgrind performance profiler.
#/   Valgrind should be available in the PATH.
#/
#/   Callgrind gathers profiling information, which then can be loaded with
#/   the `KCachegrind` tool to visualize and interpret it.
#/
#/   See:
#/   * Callgrind manual: http://valgrind.org/docs/manual/cl-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.
#/
#/ --address-sanitizer
#/
#/   Build and run with the instrumentation provided by the compiler's
#/   AddressSanitizer and LeakSanitizer (available in GCC and Clang).
#/
#/   See:
#/   * https://clang.llvm.org/docs/AddressSanitizer.html
#/   * https://clang.llvm.org/docs/LeakSanitizer.html
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.
#/
#/ --thread-sanitizer
#/
#/   Build and run with the instrumentation provided by the compiler's
#/   ThreadSanitizer (available in GCC and Clang).
#/
#/   See: https://clang.llvm.org/docs/ThreadSanitizer.html
#/
#/   NOTE: A recent version of GCC is required for ThreadSanitizer to work;
#/   GCC 5, 6 and 7 have been tested and don't work; GCC 8 and 9 do.
#/   On top of that, there's the issue of having some false positives due
#/   to the custom thread-synchronization routines from GLib (like GMutex),
#/   which TSAN doesn't understand and ends up considering as race conditions.
#/
#/   The official solution is to recompile GLib with TSAN instrumentation.
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.
#/
#/ --undefined-sanitizer
#/
#/   Build and run with the compiler's UndefinedBehaviorSanitizer, an
#/   undefined behavior detector (available in GCC and Clang).
#/
#/   See:
#/   * https://clang.llvm.org/docs/UndefinedBehaviorSanitizer.html
#/
#/   Optional. Default: Disabled.
#/   Implies '--release'.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1



# Parse call arguments
# --------------------

CFG_BUILD_ONLY="false"
CFG_RELEASE="false"
CFG_GDB="false"
CFG_CLANG="false"
CFG_VERBOSE="false"
CFG_VALGRIND_MEMCHECK="false"
CFG_VALGRIND_MASSIF="false"
CFG_VALGRIND_CALLGRIND="false"
CFG_ADDRESS_SANITIZER="false"
CFG_THREAD_SANITIZER="false"
CFG_UNDEFINED_SANITIZER="false"
CFG_KMS_ARGS=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --build-only) CFG_BUILD_ONLY="true" ;;
        --release) CFG_RELEASE="true" ;;
        --gdb) CFG_GDB="true" ;;
        --clang) CFG_CLANG="true" ;;
        --verbose) CFG_VERBOSE="true" ;;
        --valgrind-memcheck) CFG_VALGRIND_MEMCHECK="true" ;;
        --valgrind-massif) CFG_VALGRIND_MASSIF="true" ;;
        --valgrind-callgrind) CFG_VALGRIND_CALLGRIND="true" ;;
        --address-sanitizer) CFG_ADDRESS_SANITIZER="true" ;;
        --thread-sanitizer) CFG_THREAD_SANITIZER="true" ;;
        --undefined-sanitizer) CFG_UNDEFINED_SANITIZER="true" ;;
        *)
            log "Argument '${1-}' will be passed to KMS"
            CFG_KMS_ARGS+=" ${1-}"
            ;;
    esac
    shift
done



# Apply config logic
# ------------------

if [[ "$CFG_VALGRIND_MEMCHECK" == "true" ]]; then
    CFG_RELEASE="true"
fi

if [[ "$CFG_VALGRIND_MASSIF" == "true" ]]; then
    CFG_RELEASE="true"
fi

if [[ "$CFG_VALGRIND_CALLGRIND" == "true" ]]; then
    CFG_RELEASE="true"
fi

if [[ "$CFG_ADDRESS_SANITIZER" == "true" ]]; then
    CFG_RELEASE="true"
fi

if [[ "$CFG_THREAD_SANITIZER" == "true" ]]; then
    CFG_RELEASE="true"
fi

log "CFG_BUILD_ONLY=$CFG_BUILD_ONLY"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_GDB=$CFG_GDB"
log "CFG_CLANG=$CFG_CLANG"
log "CFG_VERBOSE=$CFG_VERBOSE"
log "CFG_VALGRIND_MEMCHECK=$CFG_VALGRIND_MEMCHECK"
log "CFG_VALGRIND_MASSIF=$CFG_VALGRIND_MASSIF"
log "CFG_VALGRIND_CALLGRIND=$CFG_VALGRIND_CALLGRIND"
log "CFG_ADDRESS_SANITIZER=$CFG_ADDRESS_SANITIZER"
log "CFG_THREAD_SANITIZER=$CFG_THREAD_SANITIZER"
log "CFG_UNDEFINED_SANITIZER=$CFG_UNDEFINED_SANITIZER"



# Run CMake if not done yet
# -------------------------

BUILD_VARS=()
BUILD_TYPE="Debug"
BUILD_DIR_SUFFIX=""
CMAKE_ARGS=""

if [[ "$CFG_RELEASE" == "true" ]]; then
    BUILD_TYPE="RelWithDebInfo"
fi

if [[ "$CFG_VERBOSE" == "true" ]]; then
    CMAKE_ARGS="$CMAKE_ARGS -DCMAKE_VERBOSE_MAKEFILE=ON"
fi

if [[ "$CFG_CLANG" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-clang"
    BUILD_VARS+=(
        "CC='clang'"
        "CXX='clang++'"
    )
else
    # Default dirs are assumed to be GCC, no need for a suffix
    BUILD_VARS+=(
        "CC='gcc'"
        "CXX='g++'"
    )
fi

if [[ "$CFG_ADDRESS_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-asan"
    CMAKE_ARGS="$CMAKE_ARGS -DSANITIZE_ADDRESS=ON"

    if [[ "$CFG_CLANG" == "true" ]]; then
        BUILD_VARS+=(
            "CFLAGS='${CFLAGS:-} -shared-libasan'"
            "CXXFLAGS='${CXXFLAGS:-} -shared-libasan'"
        )
    else
        BUILD_VARS+=(
            # Use flag recommended for aggressive diagnostics:
            # https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
            "CFLAGS='${CFLAGS:-} -fsanitize-address-use-after-scope'"
            "CXXFLAGS='${CXXFLAGS:-} -fsanitize-address-use-after-scope'"
        )
    fi
fi

if [[ "$CFG_THREAD_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-tsan"
    CMAKE_ARGS="$CMAKE_ARGS -DSANITIZE_THREAD=ON"
fi

if [[ "$CFG_UNDEFINED_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-ubsan"
    CMAKE_ARGS="$CMAKE_ARGS -DSANITIZE_UNDEFINED=ON"

    # FIXME: A bug in the `ld` linker (package "binutils") in Ubuntu 16.04 "Xenial"
    # makes the CMake test for UBSan compatibility to fail.
    # A simple workaround is to use `gold` instead of `ld`.
    # Clang doesn't need this, because it uses `lld`, the LLVM linker.
    # See: https://stackoverflow.com/questions/50024731/ld-unrecognized-option-push-state-no-as-needed
    #
    # The bug is fixed in Ubuntu 18.04 "Bionic". So this workaround can be
    # removed when Kurento drops support for Xenial.
    if [[ "$CFG_CLANG" != "true" ]]; then
        BUILD_VARS+=(
            "CFLAGS='${CFLAGS:-} -fuse-ld=gold'"
            "CXXFLAGS='${CXXFLAGS:-} -fuse-ld=gold'"
        )
    fi
fi

if [[ -f /.dockerenv ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-docker"
fi

BUILD_DIR="build-${BUILD_TYPE}${BUILD_DIR_SUFFIX}"

if [[ ! -f "$BUILD_DIR/kurento-media-server/server/kurento-media-server" ]]; then
    # If only a partial build exists (or none at all), delete it
    rm -rf "$BUILD_DIR"

    mkdir -p "$BUILD_DIR"
    pushd "$BUILD_DIR" || exit 1  # Enter $BUILD_DIR

    # Prepare the final command
    COMMAND=""
    for BUILD_VAR in "${BUILD_VARS[@]:-}"; do
        [[ -n "$BUILD_VAR" ]] && COMMAND="$COMMAND $BUILD_VAR"
    done

    COMMAND="$COMMAND cmake -DCMAKE_BUILD_TYPE=$BUILD_TYPE -DCMAKE_EXPORT_COMPILE_COMMANDS=ON $CMAKE_ARGS .."

    log "Run command: $COMMAND"
    eval "$COMMAND"

    popd || exit 1  # Exit $BUILD_DIR
fi

# Other Make alternatives:
# make check_build       # Build all tests
# make check             # Build and run all tests
# make <TestName>.check  # Build and run specific test
# make valgrind



# Prepare run environment
# -----------------------

RUN_VARS=()
RUN_WRAPPER=""

if [[ "$CFG_GDB" == "true" ]]; then
    # RUN_WRAPPER="gdb -ex 'run' --args"
    RUN_WRAPPER="gdb --args"
    RUN_VARS+=(
        "G_DEBUG='fatal-warnings'"

        # Prevent GStreamer from forking on startup
        "GST_REGISTRY_FORK='no'"
    )
fi

if [[ "$CFG_VALGRIND_MEMCHECK" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$BASEPATH/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=memcheck --log-file='valgrind-memcheck-%p.log' ${VALGRIND_ARGS[*]}"
    RUN_VARS+=(
        "G_DEBUG='gc-friendly'"
        #"G_SLICE='always-malloc'"
        #"G_SLICE='debug-blocks'"
        "G_SLICE='all'"

        # Prevent GStreamer from forking on startup
        "GST_REGISTRY_FORK='no'"

        # Enable deletion of MediaSet objects in KMS, to avoid leak reports
        "DEBUG_MEDIASET='1'"
    )

elif [[ "$CFG_VALGRIND_MASSIF" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$BASEPATH/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=massif --log-file='valgrind-massif-%p.log' --massif-out-file='valgrind-massif-%p.out' ${VALGRIND_ARGS[*]}"

elif [[ "$CFG_VALGRIND_CALLGRIND" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$BASEPATH/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=callgrind --log-file='valgrind-callgrind-%p.log' --callgrind-out-file='valgrind-callgrind-%p.out' ${VALGRIND_ARGS[*]}"

elif [[ "$CFG_ADDRESS_SANITIZER" == "true" ]]; then
    if [[ "$CFG_CLANG" == "true" ]]; then
        CLANG_VERSION="$(clang --version | perl -ne '/clang version (\d+\.\d+\.\d+)/ && print $1')"
        CLANG_VERSION_MAJ="$(echo "$CLANG_VERSION" | head -c1)"
        LIBSAN="/usr/lib/llvm-${CLANG_VERSION_MAJ}/lib/clang/${CLANG_VERSION}/lib/linux/libclang_rt.asan-x86_64.so"
    else
        GCC_VERSION="$(gcc -dumpversion | head -c1)"
        LIBSAN="/usr/lib/gcc/x86_64-linux-gnu/${GCC_VERSION}/libasan.so"
    fi

    RUN_VARS+=(
        "LD_PRELOAD='$LIBSAN'"
        # Use ASAN_OPTIONS recommended for aggressive diagnostics:
        # https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
        # NOTE: "detect_stack_use_after_return=1" breaks Kurento execution (more study needed to see why)
        # NOTE: GST_PLUGIN_DEFINE() causes ODR violations so this check must be disabled
        "ASAN_OPTIONS='suppressions=${PWD}/bin/sanitizers/asan.supp detect_odr_violation=0 detect_leaks=1 detect_invalid_pointer_pairs=2 strict_string_checks=1 detect_stack_use_after_return=0 check_initialization_order=1 strict_init_order=1'"

        # Enable deletion of MediaSet objects in KMS, to avoid leak reports
        "DEBUG_MEDIASET='1'"
    )

elif [[ "$CFG_THREAD_SANITIZER" == "true" ]]; then
    GCC_VERSION="$(gcc -dumpversion | head -c1)"
    LIBSAN="/usr/lib/gcc/x86_64-linux-gnu/$GCC_VERSION/libtsan.so"
    RUN_VARS+=(
        "LD_PRELOAD='$LIBSAN'"
        "TSAN_OPTIONS='suppressions=${PWD}/bin/sanitizers/tsan.supp ignore_interceptors_accesses=1 ignore_noninstrumented_modules=1'"
    )
fi

# Set default debug log settings, if none given
if [[ -n "${GST_DEBUG:-}" ]]; then
    RUN_VARS+=(
        "GST_DEBUG='$GST_DEBUG'"
    )
else
    RUN_VARS+=(
        "GST_DEBUG='2,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4'"
    )
fi

# (Optional) Extra GST_DEBUG categories
# export GST_DEBUG="${GST_DEBUG:-2},aggregator:5,compositor:5,compositemixer:5"
# export GST_DEBUG="${GST_DEBUG:-2},baseparse:6,h264parse:6"
# export GST_DEBUG="${GST_DEBUG:-2},Kurento*:5,agnosticbin*:5"
# export GST_DEBUG="${GST_DEBUG:-2},kmswebrtcsession:6"



# Run Kurento Media Server
# ------------------------

pushd "$BUILD_DIR" || exit 1  # Enter $BUILD_DIR

# Always run `make`: if any source file changed, it needs building; if nothing
# changed since last time, it is a "no-op" anyway
make -j"$(nproc)"

if [[ "$CFG_BUILD_ONLY" == "true" ]]; then
    exit 0
fi

# System limits: Set maximum open file descriptors
# Maximum limit value allowed by Ubuntu: 2^20 = 1048576
ulimit -n 1048576

# System limits: Enable kernel core dump
ulimit -c unlimited

# System config: Set path for Kernel core dump files
# NOTE: Requires root (runs with `sudo`)
#KERNEL_CORE_PATH="${PWD}/core_%e_%p_%u_%t"
#log "Set kernel core dump path: $KERNEL_CORE_PATH"
#echo "$KERNEL_CORE_PATH" | sudo tee /proc/sys/kernel/core_pattern >/dev/null

# Prepare the final command
COMMAND=""
for RUN_VAR in "${RUN_VARS[@]:-}"; do
    [[ -n "$RUN_VAR" ]] && COMMAND="$COMMAND $RUN_VAR"
done

COMMAND="$COMMAND $RUN_WRAPPER"

COMMAND="$COMMAND kurento-media-server/server/kurento-media-server \
    --conf-file='$PWD/config/kurento.conf.json' \
    --modules-config-path='$PWD/config' \
    --modules-path='$PWD:/usr/lib/x86_64-linux-gnu/kurento/modules' \
    --gst-plugin-path='$PWD:/usr/lib/x86_64-linux-gnu/gstreamer-1.5' \
"

log "Run command: $COMMAND"
eval "$COMMAND" "$CFG_KMS_ARGS"

popd || exit 1  # Exit $BUILD_DIR
