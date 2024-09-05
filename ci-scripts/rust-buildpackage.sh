#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Rust packaging script for Debian/Ubuntu.
#/
#/ This script is used to build Rust gstreamer plugins dependencies, and generate
#/ Debian/Ubuntu package files (.deb) from them. It will automatically install
#/ all required dependencies with `apt-get`, then build the project.
#/
#/ If running on a Git repository, information about the current commit will be
#/ added to the resulting version number (for development builds).
#/
#/
#/
#/ Arguments
#/ ---------
#/
#/ --dstdir <DstDir>
#/
#/   Specifies where the resulting Debian package files ('*.deb') should be
#/   placed after the build finishes.
#/
#/   Optional. Default: Current working directory.
#/
#/ --release
#/
#/   Build packages intended for Release. If this option is not given, packages
#/   are built as development snapshots.
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
#/ --apt-proxy <ProxyUrl>
#/
#/   Use the given HTTP proxy for apt-get. This can be useful in environments
#/   where such a proxy is set up, in order to save on data transfer costs from
#/   official system repositories.
#/
#/   <ProxyUrl> is set to Apt option "Acquire::http::Proxy".
#/
#/   Doc: https://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html
#/
#/ --platfomr <platform specification>
#/   
#/   Sets the target binary platform to use (e.g x86_64-linux-gnu or arm64-linux-gnu)
#/ 
#/   <platform specification> is set by default to x86_64-linux-gnu
#/
#/ --package <package>
#/
#/    Sets the package in the module that it is intended to be built




# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Check permissions
# =================

[[ "$(id -u)" -eq 0 ]] || {
    log "ERROR: Please run as root user (or with 'sudo')"
    exit 1
}



# Parse call arguments
# ====================

CFG_DSTDIR="$PWD"
CFG_RELEASE="false"
CFG_TIMESTAMP="$(date --utc +%Y%m%d%H%M%S)"
CFG_APT_PROXY_URL=""
CFG_PLATFORM="x86_64-unknown-linux-gnu"
CFG_PACKAGE="main"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --install-files)
            if [[ -n "${2-}" ]]; then
                CFG_DSTDIR="$(realpath "$2")"
                shift
            else
                log "ERROR: --dstdir expects <DstDir>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
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
        --apt-proxy)
            if [[ -n "${2-}" ]]; then
                CFG_APT_PROXY_URL="$2"
                shift
            else
                log "ERROR: --apt-proxy expects <ProxyUrl>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --platform)
            if [[ -n "${2-}" ]]; then
                CFG_PLATFORM="$2"
                shift
            else
                log "ERROR: --platform expects <Platform specification>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --package)
            if [[ -n "${2-}" ]]; then
                CFG_PACKAGE="$2"
                shift
            else
                log "ERROR: --package expects <package>"
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



# Validate config
# ===============

[[ -d "$CFG_DSTDIR" ]] || {
    log "ERROR: --dstdir given a nonexistent path: '$CFG_DSTDIR'"
    exit 1
}
log "CFG_DSTDIR=$CFG_DSTDIR"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_TIMESTAMP=$CFG_TIMESTAMP"
log "CFG_APT_PROXY_URL=$CFG_APT_PROXY_URL"
log "CFG_PLATFORM=$CFG_PLATFORM"
log "CFG_PACKAGE=$CFG_PACKAGE"



# Configure apt-get
# =================

# Get Ubuntu version definitions. This brings variables such as:
#     DISTRIB_CODENAME="focal"
#     DISTRIB_RELEASE="20.04"
source /etc/lsb-release

# Extra options for all apt-get invocations
APT_ARGS=()

# If requested, use an Apt proxy
if [[ -n "$CFG_APT_PROXY_URL" ]]; then
    APT_ARGS+=("-o")
    APT_ARGS+=("Acquire::http::Proxy=$CFG_APT_PROXY_URL")
fi



# Build packages
# ==============

BUILD_CMD_ARGS=()

if [[ -n $CFG_PLATFORM ]]; then 
    log "Building Gstreamer Rust package for platform '$CFG_PLATFORM'"
    BUILD_CMD_ARGS+=(
        "--target=$CFG_PLATFORM"
    )
fi

if [[ -n $CFG_PACKAGE ]]; then 
    log "Building Gstreamer Rust package '$CFG_PACKAGE$"
    BUILD_CMD_ARGS+=(
        "--package"
        "$CFG_PACKAGE"
    )
fi

if [[ "$CFG_RELEASE" == "true" ]]; then
    log "Running package builder for a RELEASE version"
    BUILD_CMD_ARGS+=(
        "--release"
    )
else
    log "Running package builder for a DEVELOPMENT snapshot"
fi

# Print logs from tests
export BOOST_TEST_LOG_LEVEL=test_suite
export CTEST_OUTPUT_ON_FAILURE=1

# GStreamer: Don't log with colors (avoid ANSI escape codes in test output)
export GST_DEBUG_NO_COLOR=1

cargo build "${BUILD_CMD_ARGS[@]}"

mkdir /build/deb-source
if [[ "$CFG_RELEASE" == "true" ]]; then
    cp -a /build/target/"$CFG_PLATFORM"/release/* /build/deb-source
else 
    cp -a /build/target/"$CFG_PLATFORM"/debug/* /build/deb-source
fi

# Generate .deb packages
BUILD_CMD_ARGS=()
BUILD_CMD_ARGS+=(
    "--separate-debug-symbols"
    "-v"
    "--no-build"
)

if [[ -n $CFG_PACKAGE ]]; then 
    log "Generating Gstreamer Rust package '$CFG_PACKAGE'"
    BUILD_CMD_ARGS+=(
        "--package=$CFG_PACKAGE"
    )
fi

cargo deb "${BUILD_CMD_ARGS[@]}"

# Move packages
# =============

# `dh_builddeb` puts by default the generated '.deb' files in '../'
# so move them to the target destination directory.
# Use 'find | xargs' here because we need to skip moving if the source
# and destination paths are the same.
log "Move resulting packages to destination dir: '$CFG_DSTDIR'"
find "target/debian" -maxdepth 1 -type f -name '*.*deb' \
    -not -path "$CFG_DSTDIR/*" -print0 \
| xargs -0 --no-run-if-empty mv --target-directory="$CFG_DSTDIR"
