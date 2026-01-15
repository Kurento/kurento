#!/bin/bash
set -e

# Configuration
WORKSPACE_DIR="$(pwd)"
PACKAGES_DIR="$WORKSPACE_DIR/server/packages"
DOCKERFILE="$WORKSPACE_DIR/docker/kurento-media-server_local_image/Dockerfile.local-debs"
IMAGE_TAG="kurento-media-server:local"

# Check if packages exist
if [ -z "$(ls -A "$PACKAGES_DIR"/*.deb 2>/dev/null)" ]; then
    echo "Error: No .deb packages found in $PACKAGES_DIR"
    echo "Please run build_all_docker.sh first."
    exit 1
fi

echo "Found packages in $PACKAGES_DIR"

# Determine architecture for the build (based on host or packages)
# Here we just use the host architecture since we are running locally
ARCH=$(uname -m)
if [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
    PLATFORM="linux/arm64"
else
    PLATFORM="linux/amd64"
fi

echo "Building Docker image for platform: $PLATFORM"

# Run Docker build
# We set the build context to the workspace root so we can access everything
sudo docker build \
    --platform "$PLATFORM" \
    -f "$DOCKERFILE" \
    -t "$IMAGE_TAG" \
    "$WORKSPACE_DIR"

echo ""
echo "Build success! Image '$IMAGE_TAG' created."
echo "Run it with:"
echo "  docker run --rm --network host $IMAGE_TAG"
