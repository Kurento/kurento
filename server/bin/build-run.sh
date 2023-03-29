#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Build and run Kurento Media Server.
#/
#/ This shell script builds KMS, and/or runs it with default options if
#/ already built.
#/
#/ To use, first clone the Kurento repo and init its submodules:
#/
#/   git clone https://github.com/Kurento/kurento.git
#/   cd kurento/server/
#/   git submodule update --init --recursive
#/
#/ Then run this script directly from that directory:
#/
#/   bin/build-run.sh
#/
#/
#/ Arguments
#/ =========
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
#/ --jemalloc
#/
#/   Run Kurento with the Jemalloc memory allocator. This improves memory
#/   handling by reducing fragmentation wrt. the standard system allocator.
#/   Requires installing Jemalloc (package `libjemalloc2` on Ubuntu 20.04).
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
#/   Note: You are in charge of installing Clang (and its symbolizer, if using
#/   any of the Sanitizers). If the programs are called `clang` and `clang++`,
#/   they will be picked by this script. Otherwise, you should pass the correct
#/   name through the env vars `CC` and `CXX`. For example:
#/
#/     sudo apt-get update ; sudo apt-get install --yes clang-12 llvm-12
#/     CC=clang-12 CXX=clang++-12 bin/build-run.sh --clang
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
#/   Memcheck manual: http://valgrind.org/docs/manual/mc-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies `--release`.
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
#/     ms_print valgrind-massif-13522.out >valgrind-massif-13522.out.txt
#/
#/   Massif manual: http://valgrind.org/docs/manual/ms-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies `--release`.
#/
#/ --valgrind-callgrind
#/
#/   Build and run with Valgrind's Callgrind performance profiler.
#/   Valgrind should be available in the PATH.
#/
#/   Callgrind gathers profiling information, which then can be loaded with
#/   the `KCachegrind` tool to visualize and interpret it.
#/
#/   Callgrind manual: http://valgrind.org/docs/manual/cl-manual.html
#/
#/   Optional. Default: Disabled.
#/   Implies `--release`.
#/
#/ --address-sanitizer
#/
#/   Build and run with the instrumentation provided by the compiler's
#/   AddressSanitizer and LeakSanitizer (available in GCC and Clang).
#/
#/   Doc:
#/
#/   * https://clang.llvm.org/docs/AddressSanitizer.html
#/   * https://clang.llvm.org/docs/LeakSanitizer.html
#/
#/   Optional. Default: Disabled.
#/   Implies `--release`.
#/
#/ --thread-sanitizer
#/
#/   Build and run with the instrumentation provided by the compiler's
#/   ThreadSanitizer (available in GCC and Clang).
#/
#/   Doc: https://clang.llvm.org/docs/ThreadSanitizer.html
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
#/   Implies `--release`.
#/
#/ --undefined-sanitizer
#/
#/   Build and run with the compiler's UndefinedBehaviorSanitizer, an
#/   undefined behavior detector (available in GCC and Clang).
#/
#/   Doc: https://clang.llvm.org/docs/UndefinedBehaviorSanitizer.html
#/
#/   Optional. Default: Disabled.
#/   Implies `--release`.



# Configure shell
# ===============

# Absolute Canonical Path to the directory that contains this script.
SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/../../ci-scripts/bash.conf.sh" || exit 1

# Trace all commands (to stderr).
#set -o xtrace



# Parse call arguments
# ====================

CFG_BUILD_ONLY="false"
CFG_RELEASE="false"
CFG_JEMALLOC="false"
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
        --jemalloc) CFG_JEMALLOC="true" ;;
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



# Validate config
# ===============

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

if [[ -n "${CC:-}" || -n "${CXX:-}" ]]; then
    if [[ -z "${CC:-}" || -z "${CXX:-}" ]]; then
        log "ERROR: Both of these env vars should be set: CC='${CC:-}' CXX='${CXX:-}'"
        exit 1
    fi

    log "Using compiler set by env vars: CC='$CC' CXX='$CXX'"

    if [[ "$CC" =~ 'gcc' ]]; then
        CFG_CLANG="false"
    elif [[ "$CC" =~ 'clang' ]]; then
        CFG_CLANG="true"
    fi
fi

log "CFG_BUILD_ONLY=$CFG_BUILD_ONLY"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_JEMALLOC=$CFG_JEMALLOC"
log "CFG_GDB=$CFG_GDB"
log "CFG_CLANG=$CFG_CLANG"
log "CFG_VERBOSE=$CFG_VERBOSE"
log "CFG_VALGRIND_MEMCHECK=$CFG_VALGRIND_MEMCHECK"
log "CFG_VALGRIND_MASSIF=$CFG_VALGRIND_MASSIF"
log "CFG_VALGRIND_CALLGRIND=$CFG_VALGRIND_CALLGRIND"
log "CFG_ADDRESS_SANITIZER=$CFG_ADDRESS_SANITIZER"
log "CFG_THREAD_SANITIZER=$CFG_THREAD_SANITIZER"
log "CFG_UNDEFINED_SANITIZER=$CFG_UNDEFINED_SANITIZER"



# Run CMake (if needed)
# =====================

BUILD_VARS=()
BUILD_TYPE="Debug"
BUILD_DIR_SUFFIX=""
CMAKE_ARGS=()

if [[ "$CFG_RELEASE" == "true" ]]; then
    BUILD_TYPE="RelWithDebInfo"
fi

if [[ "$CFG_VERBOSE" == "true" ]]; then
    CMAKE_ARGS+=("-DCMAKE_VERBOSE_MAKEFILE=TRUE")
fi

if [[ "$CFG_CLANG" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-clang"
    CC="${CC:-clang}"
    CXX="${CXX:-clang++}"
else
    CC="${CC:-gcc}"
    CXX="${CXX:-g++}"
fi

if [[ "$CFG_ADDRESS_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-asan"
    CMAKE_ARGS+=("-DSANITIZE_ADDRESS=TRUE")

    if [[ "$CFG_CLANG" == "true" ]]; then
        # While GCC opts for shared sanitizer libs by default, Clang goes the
        # other way and prefers statically linking them, unless told otherwise.
        CFLAGS+=" -shared-libsan"
        CXXFLAGS+=" -shared-libsan"
    fi

    CXXFLAGS+=" -fsized-deallocation"
fi

if [[ "$CFG_THREAD_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-tsan"
    CMAKE_ARGS+=("-DSANITIZE_THREAD=TRUE")
fi

if [[ "$CFG_UNDEFINED_SANITIZER" == "true" ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-ubsan"
    CMAKE_ARGS+=("-DSANITIZE_UNDEFINED=TRUE")
fi

if [[ -f /.dockerenv ]]; then
    BUILD_DIR_SUFFIX="${BUILD_DIR_SUFFIX}-docker"
fi

# Store vars that affect compiler configuration.
BUILD_VARS+=(
    # C
    "CC='${CC:-}'"
    "CFLAGS='${CFLAGS:-}'"

    # C++
    "CXX='${CXX:-}'"
    "CXXFLAGS='${CXXFLAGS:-}'"
)

BUILD_DIR="build-${BUILD_TYPE}${BUILD_DIR_SUFFIX}"

# Extra CMake args that are always set.
CMAKE_ARGS+=(
    "-DCMAKE_BUILD_TYPE=$BUILD_TYPE"
    "-DDISABLE_IPV6_TESTS=FALSE"
    "-DCMAKE_EXPORT_COMPILE_COMMANDS=TRUE"
)

if [[ ! -f "$BUILD_DIR/media-server/server/kurento-media-server" ]]; then
    # If only a partial build exists (or none at all), delete it.
    rm -rf "$BUILD_DIR"

    mkdir -p "$BUILD_DIR"
    pushd "$BUILD_DIR" || exit 1  # Enter $BUILD_DIR

    # Prepare the build command.
    COMMAND=""
    for BUILD_VAR in "${BUILD_VARS[@]:-}"; do
        [[ -n "$BUILD_VAR" ]] && COMMAND="$COMMAND $BUILD_VAR"
    done

    COMMAND="$COMMAND cmake ${CMAKE_ARGS[*]} .."

    log "Run command: $COMMAND"
    eval "$COMMAND"

    popd || exit 1  # Exit $BUILD_DIR
fi



# Prepare run command
# ===================

# GLib settings: https://docs.gtk.org/glib/running.html#environment-variables

RUN_VARS=()
RUN_WRAPPER=""

if [[ "$CFG_JEMALLOC" == "true" ]]; then
    # Find the full path to the Jemalloc library file.
    JEMALLOC_PATH="$(find /usr/lib/x86_64-linux-gnu/ | grep 'libjemalloc\.so\.[0-9]+' | sort --version-sort --reverse | head --lines 1)" || {
        log "ERROR: Jemalloc not found, please install it:"
        log "sudo apt-get update && sudo apt-get install '^libjemalloc[0-9]+$'"
        exit 1
    }

    JEMALLOC_CONF="abort_conf:true,confirm_conf:true"
    #JEMALLOC_CONF="abort_conf:true,confirm_conf:true,background_thread:true,metadata_thp:auto"

    RUN_VARS+=(
        "LD_PRELOAD='$JEMALLOC_PATH'"
        "MALLOC_CONF='$JEMALLOC_CONF'"

        # gc-friendly: Initialize memory with 0s. Useful for memory checkers.
        "G_DEBUG='gc-friendly'"

        # always-malloc: Disable custom allocator and use `malloc` instead. Useful for memory checkers.
        "G_SLICE='always-malloc'"
    )
fi

if [[ "$CFG_GDB" == "true" ]]; then
    RUN_WRAPPER="gdb --args"
    RUN_VARS+=(
        # fatal-warnings: Abort on calls to `g_warning()`` or `g_critical()`.
        "G_DEBUG='fatal-warnings'"

        # Prevent GStreamer from forking at startup.
        "GST_REGISTRY_FORK='no'"
    )
fi

if [[ "$CFG_VALGRIND_MEMCHECK" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$SELF_DIR/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=memcheck --log-file='valgrind-memcheck-%p.log' ${VALGRIND_ARGS[*]}"
    RUN_VARS+=(
        # gc-friendly: Initialize memory with 0s. Useful for memory checkers.
        "G_DEBUG='gc-friendly'"

        # always-malloc: Disable custom allocator and use `malloc` instead. Useful for memory checkers.
        # debug-blocks: Enable sanity checks on released memory slices.
        "G_SLICE='always-malloc,debug-blocks'"

        # Prevent GStreamer from forking at startup
        "GST_REGISTRY_FORK='no'"
    )

elif [[ "$CFG_VALGRIND_MASSIF" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$SELF_DIR/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=massif --log-file='valgrind-massif-%p.log' --massif-out-file='valgrind-massif-%p.out' ${VALGRIND_ARGS[*]}"

elif [[ "$CFG_VALGRIND_CALLGRIND" == "true" ]]; then
    # shellcheck source=valgrind.conf.sh
    source "$SELF_DIR/valgrind.conf.sh" || exit 1
    RUN_WRAPPER="valgrind --tool=callgrind --log-file='valgrind-callgrind-%p.log' --callgrind-out-file='valgrind-callgrind-%p.out' ${VALGRIND_ARGS[*]}"

elif [[ "$CFG_ADDRESS_SANITIZER" == "true" ]]; then
    if [[ "$CFG_CLANG" == "true" ]]; then
        LIBSAN="$("$CC" -print-file-name=libclang_rt.asan-x86_64.so)"
    else
        LIBSAN="$("$CC" -print-file-name=libasan.so)"
    fi

    ASAN_OPTIONS="suppressions=$PWD/bin/sanitizers/asan.supp"
    # Use ASAN_OPTIONS recommended for aggressive diagnostics:
    # https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
    ASAN_OPTIONS+=":strict_string_checks=1"
    ASAN_OPTIONS+=":detect_stack_use_after_return=1"
    ASAN_OPTIONS+=":check_initialization_order=1"
    ASAN_OPTIONS+=":strict_init_order=1"
    # FIXME: GST_PLUGIN_DEFINE() causes ODR violations so this check must be disabled.
    ASAN_OPTIONS+=":detect_odr_violation=0"
    # FIXME: `new_delete_type_mismatch=0` is needed because libsigc++ contains false positives
    #     and ASan doesn't provide a granular way of suppressing them.
    #     See discussion here: https://github.com/libsigcplusplus/libsigcplusplus/issues/10
    #     See feature request here: https://github.com/llvm/llvm-project/issues/58404
    ASAN_OPTIONS+=":new_delete_type_mismatch=0"
    # Extra options for more comprehensive memory analysis.
    ASAN_OPTIONS+=":detect_leaks=1"
    ASAN_OPTIONS+=":detect_invalid_pointer_pairs=2"

    RUN_VARS+=(
        "LD_PRELOAD='$LIBSAN'"
        "ASAN_OPTIONS='$ASAN_OPTIONS'"

        # gc-friendly: Initialize memory with 0s. Useful for memory checkers.
        "G_DEBUG='gc-friendly'"

        # always-malloc: Disable custom allocator and use `malloc` instead. Useful for memory checkers.
        # debug-blocks: Enable sanity checks on released memory slices.
        "G_SLICE='always-malloc,debug-blocks'"

        # Prevent GStreamer from forking at startup
        "GST_REGISTRY_FORK='no'"
    )

elif [[ "$CFG_THREAD_SANITIZER" == "true" ]]; then
    if [[ "$CFG_CLANG" == "true" ]]; then
        # Nothing to do: Clang only ships a static lib for TSan.
        true
    else
        LIBSAN="$("$CC" -print-file-name=libtsan.so)"

        RUN_VARS+=(
            "LD_PRELOAD='$LIBSAN'"
        )
    fi

    RUN_VARS+=(
        "TSAN_OPTIONS='suppressions=${PWD}/bin/sanitizers/tsan.supp ignore_interceptors_accesses=1 ignore_noninstrumented_modules=1'"

        # gc-friendly: Initialize memory with 0s. Useful for memory checkers.
        "G_DEBUG='gc-friendly'"

        # always-malloc: Disable custom allocator and use `malloc` instead. Useful for memory checkers.
        # debug-blocks: Enable sanity checks on released memory slices.
        "G_SLICE='always-malloc,debug-blocks'"

        # Prevent GStreamer from forking at startup
        "GST_REGISTRY_FORK='no'"
    )
fi

# Pass debug log settings, or defaults if none are set.
if [[ -n "${GST_DEBUG:-}" ]]; then
    RUN_VARS+=("GST_DEBUG='$GST_DEBUG'")
else
    RUN_VARS+=("GST_DEBUG='2,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4'")
fi

# Pass other relevant environment variables, if set.
if [[ -n "${GST_DEBUG_DUMP_DOT_DIR:-}" ]]; then
    RUN_VARS+=("GST_DEBUG_DUMP_DOT_DIR='$GST_DEBUG_DUMP_DOT_DIR'")
fi



# Launch run command
# ==================

pushd "$BUILD_DIR" || exit 1  # Enter $BUILD_DIR

# Always run `make`: if any source file changed, it needs building; if nothing
# changed since last time, it is a "no-op" anyway.
#
# Other Make alternatives:
#     make check_build       # Build all tests
#     make check             # Build and run all tests
#     make <TestName>.check  # Build and run specific test
#     make valgrind
if [[ -n "${MAKEFLAGS:-}" ]]; then
    # Custom Make flags are already set in the environment. Let Make use them.
    make
else
    make -j"$(nproc)"
fi

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

# Set modules path.
# Equivalent to `--modules-path`, `--modules-config-path`, `--gst-plugin-path`.
RUN_VARS+=(
    # Use the former to include modules already installed in the system.
    #"KURENTO_MODULES_PATH='${KURENTO_MODULES_PATH:+$KURENTO_MODULES_PATH:}$PWD:/usr/lib/x86_64-linux-gnu/kurento/modules'"
    "KURENTO_MODULES_PATH='${KURENTO_MODULES_PATH:+$KURENTO_MODULES_PATH:}$PWD'"

    "KURENTO_MODULES_CONFIG_PATH='${KURENTO_MODULES_CONFIG_PATH:+$KURENTO_MODULES_CONFIG_PATH:}$PWD/config'"

    # Use the former to include plugins already installed in the system.
    #"GST_PLUGIN_PATH='${GST_PLUGIN_PATH:+$GST_PLUGIN_PATH:}$PWD:/usr/lib/x86_64-linux-gnu/gstreamer-1.0'"
    "GST_PLUGIN_PATH='${GST_PLUGIN_PATH:+$GST_PLUGIN_PATH:}$PWD'"
)

# Use `env` to set the environment variables just for our target program,
# without affecting the wrapper.
COMMAND="$RUN_WRAPPER env --ignore-environment"
for RUN_VAR in "${RUN_VARS[@]}"; do
    [[ -n "$RUN_VAR" ]] && COMMAND+=" $RUN_VAR"
done

COMMAND+=" media-server/server/kurento-media-server \
    --conf-file='$PWD/config/kurento.conf.json' \
"

log "Run command: $COMMAND"
eval "$COMMAND" "$CFG_KMS_ARGS"

popd || exit 1  # Exit $BUILD_DIR
