#!/bin/bash
set -e

# Configuration
WORKSPACE_DIR="$(pwd)"
PACKAGES_BASE_DIR="$WORKSPACE_DIR/server/packages"
DOCKERFILE="$WORKSPACE_DIR/docker/kurento-media-server_local_image/Dockerfile.local-debs"
IMAGE_TAG="${IMAGE_TAG:-kurento-media-server:local}"

# Parse command line arguments
BUILD_MODE="${1:-multi}"  # multi, single, or specific arch (amd64/arm64)

echo "================================================================"
echo "Multi-Architecture Docker Image Builder"
echo "================================================================"

# Determine which architectures to build
case "$BUILD_MODE" in
    multi)
        PLATFORMS="linux/amd64,linux/arm64"
        ARCHITECTURES="amd64 arm64"
        echo "Building multi-architecture image for: amd64, arm64"
        ;;
    amd64|x86_64)
        PLATFORMS="linux/amd64"
        ARCHITECTURES="amd64"
        echo "Building single architecture image for: amd64"
        ;;
    arm64|aarch64)
        PLATFORMS="linux/arm64"
        ARCHITECTURES="arm64"
        echo "Building single architecture image for: arm64"
        ;;
    single)
        # Build for host architecture only
        ARCH=$(uname -m)
        if [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
            PLATFORMS="linux/arm64"
            ARCHITECTURES="arm64"
        else
            PLATFORMS="linux/amd64"
            ARCHITECTURES="amd64"
        fi
        echo "Building for host architecture: $ARCHITECTURES"
        ;;
    *)
        echo "Error: Unknown build mode '$BUILD_MODE'"
        echo "Usage: $0 [multi|single|amd64|arm64]"
        echo "  multi  - Build for both amd64 and arm64 (default)"
        echo "  single - Build for host architecture only"
        echo "  amd64  - Build for amd64 only"
        echo "  arm64  - Build for arm64 only"
        exit 1
        ;;
esac

echo "================================================================"
echo ""

# Check if packages exist for all requested architectures
MISSING_ARCH=""
for arch in $ARCHITECTURES; do
    PACKAGES_DIR="$PACKAGES_BASE_DIR/$arch"
    if [ ! -d "$PACKAGES_DIR" ] || [ -z "$(ls -A "$PACKAGES_DIR"/*.deb 2>/dev/null)" ]; then
        echo "Warning: No .deb packages found in $PACKAGES_DIR"
        MISSING_ARCH="$MISSING_ARCH $arch"
    else
        echo "Found packages for $arch in $PACKAGES_DIR"
    fi
done

if [ -n "$MISSING_ARCH" ]; then
    echo ""
    echo "Error: Missing packages for architectures:$MISSING_ARCH"
    echo "Please run: ./build_all_docker.sh"
    exit 1
fi

echo ""
echo "Building Docker image(s)..."
echo "Image tag: $IMAGE_TAG"
echo "Platforms: $PLATFORMS"
echo ""

# Setup Docker buildx builder if needed
if ! docker buildx inspect kurento-builder >/dev/null 2>&1; then
    echo "Creating Docker buildx builder 'kurento-builder'..."
    docker buildx create --name kurento-builder --use --platform "$PLATFORMS"
else
    echo "Using existing Docker buildx builder 'kurento-builder'..."
    docker buildx use kurento-builder
fi

# Check if multi-architecture manifest build is requested
if [[ "$PLATFORMS" == *","* ]]; then
    echo ""
    echo "Building multi-architecture manifest image..."
    echo "Note: This will push to a registry or use --load is not supported for multi-arch."
    echo "Building individual images per architecture instead..."
    echo ""

    # Build separate images for each architecture
    IFS=' ' read -ra ARCH_ARRAY <<< "$ARCHITECTURES"
    for arch in "${ARCH_ARRAY[@]}"; do
        echo "================================================================"
        echo "Building image for $arch..."
        echo "================================================================"

        docker buildx build \
            --platform "linux/$arch" \
            --load \
            --build-arg ARCH="$arch" \
            --build-arg PACKAGES_DIR="server/packages/$arch" \
            -f "$DOCKERFILE" \
            -t "${IMAGE_TAG}-${arch}" \
            "$WORKSPACE_DIR"

        echo ""
        echo "Image ${IMAGE_TAG}-${arch} created successfully!"
        echo "Run it with: docker run --rm --network host ${IMAGE_TAG}-${arch}"
        echo ""
    done

    echo "================================================================"
    echo "Multi-Architecture Build Complete!"
    echo "================================================================"
    echo "Created images:"
    for arch in "${ARCH_ARRAY[@]}"; do
        echo "  - ${IMAGE_TAG}-${arch}"
    done
    echo ""
    echo "To create a multi-arch manifest (requires registry push):"
    echo "  docker manifest create $IMAGE_TAG \\"
    for arch in "${ARCH_ARRAY[@]}"; do
        echo "    ${IMAGE_TAG}-${arch} \\"
    done
    echo ""

else
    # Single architecture build
    arch="${PLATFORMS##*/}"

    echo "Building single-architecture image..."
    docker buildx build \
        --platform "$PLATFORMS" \
        --load \
        --build-arg ARCH="$arch" \
        --build-arg PACKAGES_DIR="server/packages/$arch" \
        -f "$DOCKERFILE" \
        -t "$IMAGE_TAG" \
        "$WORKSPACE_DIR"

    echo ""
    echo "================================================================"
    echo "Build success! Image '$IMAGE_TAG' created."
    echo "================================================================"
    echo "Run it with:"
    echo "  docker run --rm --network host $IMAGE_TAG"
    echo ""
fi
