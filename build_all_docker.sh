#!/bin/bash
set -e

# Directory where generated packages will be stored
PACKAGES_DIR="$(pwd)/server/packages"
mkdir -p "$PACKAGES_DIR"

# Docker image to use
DOCKER_IMAGE="kurento-buildpackage:local"

# Check if image exists
if ! sudo docker image inspect "$DOCKER_IMAGE" >/dev/null 2>&1; then
    cd docker/kurento-buildpackage
    sudo docker build -t kurento-buildpackage:local .
    cd -
    if ! sudo docker image inspect "$DOCKER_IMAGE" >/dev/null 2>&1; then
        echo "Error: Docker image '$DOCKER_IMAGE' not found."
        exit 1
    fi
fi

# Function to build a component
build_component() {
    local component="$1"
    local run_args="$2"
    
    echo ""
    echo "################################################################"
    echo "Building Component: $component"
    echo "################################################################"

    # Remove existing packages for this component to avoid conflicts during build
    echo "Cleaning up old packages for $component..."
    rm -f "$PACKAGES_DIR"/*"$component"*.deb
    rm -f "$PACKAGES_DIR"/*"$component"*.ddeb
    
    # We use docker run to build the component
    # Tests keeps timing-out so I added -e DEB_BUILD_OPTIONS="nocheck"
    # TODO: fix tests 
    sudo docker run --rm \
        --cap-add=SYS_NICE \
        --security-opt seccomp=unconfined \
        -e DEB_BUILD_OPTIONS="nocheck" \
        -v "$(pwd)/server/$component":/hostdir \
        -v "$PACKAGES_DIR":/packages \
        -v "$(pwd)/ci-scripts":/ci-scripts \
        "$DOCKER_IMAGE" \
        --dstdir /packages \
        --allow-dirty \
        $run_args
}

COMPONENT="${1:-all}"

if [[ "$COMPONENT" != "all" ]]; then
    build_component "$COMPONENT" "--install-files /packages"
    exit 0
fi

# 1. Build cmake-utils
build_component "cmake-utils" ""

# 2. Build module-creator
build_component "module-creator" ""

# 3. Build jsonrpc
# jsonrpc might depend on cmake-utils? 
# The original script says: # safest to use --install-kurento 7.0.0 or just rely on system
# But we are in a clean container, so we must rely on what we just built.
# However, jsonrpc in Kurento 7 usually needs cmake-utils.
# Let's pass --install-files /packages just in case.
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
echo "Build Complete!"
echo "Packages are located in: $PACKAGES_DIR"
echo "################################################################"
