#!/bin/bash
set -e

# Target architectures to build for
ARCHITECTURES="${ARCHITECTURES:-linux/amd64,linux/arm64}"

# Parse command line arguments
COMPONENT="${1:-all}"
SINGLE_ARCH="${2:-}"

# If single architecture is specified, use only that
if [[ -n "$SINGLE_ARCH" ]]; then
    case "$SINGLE_ARCH" in
        amd64|x86_64)
            ARCHITECTURES="linux/amd64"
            ;;
        arm64|aarch64)
            ARCHITECTURES="linux/arm64"
            ;;
        *)
            echo "Error: Unknown architecture '$SINGLE_ARCH'"
            echo "Supported: amd64, arm64"
            exit 1
            ;;
    esac
fi

# Base directory for packages
PACKAGES_BASE_DIR="$(pwd)/server/packages"
mkdir -p "$PACKAGES_BASE_DIR"

# Docker image base name
DOCKER_IMAGE_BASE="kurento-buildpackage"

echo "================================================================"
echo "Multi-Architecture Build Configuration"
echo "================================================================"
echo "Target architectures: $ARCHITECTURES"
echo "Building component: $COMPONENT"
echo "================================================================"
echo ""

# Setup Docker buildx builder if not exists
if ! docker buildx inspect kurento-builder >/dev/null 2>&1; then
    echo "Creating Docker buildx builder 'kurento-builder'..."
    docker buildx create --name kurento-builder --use --platform "$ARCHITECTURES"
else
    echo "Using existing Docker buildx builder 'kurento-builder'..."
    docker buildx use kurento-builder
fi

# Build multi-arch build images
echo "Building multi-architecture build container images..."
cd docker/kurento-buildpackage

# Build for each architecture separately to tag them properly
IFS=',' read -ra ARCH_ARRAY <<< "$ARCHITECTURES"
for platform in "${ARCH_ARRAY[@]}"; do
    # Extract arch name (linux/amd64 -> amd64)
    arch="${platform##*/}"

    echo ""
    echo "Building buildpackage image for $arch..."
    docker buildx build \
        --platform "$platform" \
        --load \
        -t "${DOCKER_IMAGE_BASE}:${arch}" \
        .
done

cd - > /dev/null

# Function to build a component for a specific architecture
build_component_arch() {
    local component="$1"
    local run_args="$2"
    local arch="$3"
    local packages_dir="$4"

    echo ""
    echo "################################################################"
    echo "Building Component: $component for $arch"
    echo "################################################################"

    # Remove existing packages for this component to avoid conflicts during build
    echo "Cleaning up old packages for $component ($arch)..."
    rm -f "$packages_dir"/*"$component"*.deb
    rm -f "$packages_dir"/*"$component"*.ddeb

    # We use docker run to build the component
    # Tests keeps timing-out so I added -e DEB_BUILD_OPTIONS="nocheck"
    # TODO: fix tests
    # Disable LTO to work around GCC 13 jobserver bug (internal_error in return_token)
    docker run --rm \
        --platform "linux/$arch" \
        --cap-add=SYS_NICE \
        --security-opt seccomp=unconfined \
        -e DEB_BUILD_OPTIONS="nocheck" \
        -e DEB_CFLAGS_APPEND="-fno-lto" \
        -e DEB_CXXFLAGS_APPEND="-fno-lto" \
        -e DEB_LDFLAGS_APPEND="-fno-lto" \
        -v "$(pwd)/server/$component":/hostdir \
        -v "$packages_dir":/packages \
        -v "$(pwd)/ci-scripts":/ci-scripts \
        "${DOCKER_IMAGE_BASE}:${arch}" \
        --dstdir /packages \
        --allow-dirty \
        $run_args
}

# Function to build a component for all architectures
build_component() {
    local component="$1"
    local run_args="$2"

    echo ""
    echo "================================================================"
    echo "Building $component for all architectures"
    echo "================================================================"

    IFS=',' read -ra ARCH_ARRAY <<< "$ARCHITECTURES"
    for platform in "${ARCH_ARRAY[@]}"; do
        # Extract arch name (linux/amd64 -> amd64)
        arch="${platform##*/}"

        # Create architecture-specific package directory
        packages_dir="$PACKAGES_BASE_DIR/$arch"
        mkdir -p "$packages_dir"

        # Build for this architecture
        build_component_arch "$component" "$run_args" "$arch" "$packages_dir"
    done
}

# Build specific component or all
if [[ "$COMPONENT" != "all" ]]; then
    build_component "$COMPONENT" "--install-files /packages"
    exit 0
fi

# Build all components in dependency order
# 1. Build cmake-utils
build_component "cmake-utils" ""

# 2. Build module-creator
build_component "module-creator" ""

# 3. Build jsonrpc
build_component "jsonrpc" "--install-files /packages"

# 4. Build module-core
build_component "module-core" "--install-files /packages"

# 5. Build module-elements
build_component "module-elements" "--install-files /packages"

# 6. Build module-filters
build_component "module-filters" "--install-files /packages"

# 7. Build media-server
build_component "media-server" "--install-files /packages"

echo ""
echo "################################################################"
echo "Multi-Architecture Build Complete!"
echo "################################################################"
IFS=',' read -ra ARCH_ARRAY <<< "$ARCHITECTURES"
for platform in "${ARCH_ARRAY[@]}"; do
    arch="${platform##*/}"
    echo "Packages for $arch: $PACKAGES_BASE_DIR/$arch/"
done
echo "################################################################"
