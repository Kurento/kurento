#!/usr/bin/env bash

#/ CI job - Generate a Debian package from the GSStreamer Rust plugins.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to create Debian packages for their projects.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from shell execution:
#/
#/ DISABLE_APT_PROXY
#/
#/   Set to "true" to skip using the Apt proxy URL.
#/   Added for GitHub Actions: No proxy is needed.
#/   Optional.
#/
#/ APT_PROXY_URL
#/
#/   Sets the URL that points to the Apt proxy to be used (if not diabled)
#/   Madatory if DISABLE_APT_PROXY is set to false
#/
#/ INSTALL_PATH
#/
#/   Path where to find the packages that should be installed.
#/   Added for GitHub Actions: The path is not ".".
#/   Optional.
#/
#/
#/ * Variable(s) from job parameters (with "This project is parameterized"):
#/
#/ JOB_RELEASE
#/
#/   "true" for release versions. "false" for nightly snapshot builds.
#/
#/ JOB_TIMESTAMP
#/
#/   Numeric timestamp shown in the version of nightly packages.
#/
#/ JOB_GIT_REPO
#/
#/   Git REpository containing the rust project to build package
#/
#/ JOB_GIT_NAME
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/ GSTREAMER_RUST_PATCH_DIR
#/
#/   Path to the diffs to apply to gstreamer rust
#/
#/ JOB_PACKAGE_NAME
#/
#/   Name of the Rust package (crate) to build as a Debian package.
#/   Default: "gst-plugin-rtp".
#/   Optional.
#/
#/ * Variable(s) from job Multi-Configuration ("Matrix") Project axis:
#/
#/ JOB_DISTRO
#/
#/   Name of the Ubuntu distribution where this job is run.
#/   E.g.: "focal".
#/
#/
#/ * Variable(s) from job Custom Tools (with "Install custom tools"):
#/
#/ KURENTO_SCRIPTS_HOME
#/
#/   Jenkins path to 'ci-scripts', containing all Kurento CI scripts.
#/



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Check out the requested branch
# Use JOB_PACKAGE_NAME if set, otherwise fall back to the default package.
GST_RUST_PACKAGE="${JOB_PACKAGE_NAME:-gst-plugin-rtp}"
CURRENT_PWD=$PWD

# Apply diff / metadata to generate deb package
if [ -d xxtmpRepoxx ]; then rm -rf xxtmpRepoxx; fi
mkdir xxtmpRepoxx
cd xxtmpRepoxx
git clone $JOB_GIT_REPO
cd *
git checkout $JOB_GIT_NAME

# Support two patching modes:
#   1. debian.diff  – a classic git diff applied with `git apply`
#   2. cargo_append.toml – TOML content appended to the package's Cargo.toml
if [[ -f "$GSTREAMER_RUST_PATCH_DIR/debian.diff" ]]; then
    git apply "$GSTREAMER_RUST_PATCH_DIR/debian.diff"
elif [[ -f "$GSTREAMER_RUST_PATCH_DIR/cargo_append.toml" ]]; then
    # Locate the Cargo.toml for the target package (works for both flat and
    # workspace-structured repositories).
    CARGO_TOML_PATH=$(find . -maxdepth 4 -name Cargo.toml \
        -exec grep -l "^name = \"${GST_RUST_PACKAGE}\"" {} \; | head -n 1)
    if [[ -z "$CARGO_TOML_PATH" ]]; then
        log "ERROR: Could not find Cargo.toml for package '${GST_RUST_PACKAGE}'"
        exit 1
    fi
    log "Appending cargo_append.toml to ${CARGO_TOML_PATH}"
    if grep -q "@DEB_GNU_ARCH_TRIPLET@" "$GSTREAMER_RUST_PATCH_DIR/cargo_append.toml"; then
        case "${JOB_ARCH:-}" in
            amd64)
                DEB_GNU_ARCH_TRIPLET="x86_64-linux-gnu"
                ;;
            arm64)
                DEB_GNU_ARCH_TRIPLET="aarch64-linux-gnu"
                ;;
            *)
                log "ERROR: Unsupported JOB_ARCH '${JOB_ARCH:-}' for @DEB_GNU_ARCH_TRIPLET@ placeholder"
                exit 1
                ;;
        esac
        sed "s|@DEB_GNU_ARCH_TRIPLET@|$DEB_GNU_ARCH_TRIPLET|g" \
            "$GSTREAMER_RUST_PATCH_DIR/cargo_append.toml" >> "$CARGO_TOML_PATH"
    else
        cat "$GSTREAMER_RUST_PATCH_DIR/cargo_append.toml" >> "$CARGO_TOML_PATH"
    fi
else
    log "WARNING: No debian.diff or cargo_append.toml found in ${GSTREAMER_RUST_PATCH_DIR}, skipping patch step"
fi


# Arguments to kurento-buildpackage.
KURENTO_BUILDPACKAGE_ARGS=()

if [[ "$JOB_RELEASE" == "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--release)
fi

if [[ "${DISABLE_APT_PROXY:-}" != "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--apt-proxy "$APT_PROXY_URL")
fi
if [[ -n "$GST_RUST_PACKAGE" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--package "$GST_RUST_PACKAGE")
fi

# Configure the platform argument
if [[ -n "${JOB_ARCH:-}" ]]; then
    if [[ "$JOB_ARCH" == "amd64" ]]; then
        KURENTO_BUILDPACKAGE_ARGS+=(--platform "x86_64-unknown-linux-gnu")
    elif [[ "$JOB_ARCH" == "arm64" ]]; then
        KURENTO_BUILDPACKAGE_ARGS+=(--platform "aarch64-unknown-linux-gnu")
    else
        log "WARNING: Unknown JOB_ARCH '$JOB_ARCH', using default platform"
    fi
fi

DOCKER_ARGS=(--pull always --rm)
if [[ -n "${JOB_ARCH:-}" ]]; then
    DOCKER_ARGS+=(--platform "linux/$JOB_ARCH")
fi

# Build
# -----

docker run "${DOCKER_ARGS[@]}" \
    --mount type=bind,src="$PWD",dst=/hostdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/ci-scripts \
    --mount type=bind,src="${INSTALL_PATH:-$PWD}",dst=/packages \
    "kurento/rust-buildpackage:${JOB_DISTRO}" \
        --timestamp "$JOB_TIMESTAMP" \
        "${KURENTO_BUILDPACKAGE_ARGS[@]}"

mv "$PWD"/*.*deb "$CURRENT_PWD"
cd ..
rm -rf xxtmpRepoxx

log "==================== END ===================="
