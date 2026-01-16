#!/usr/bin/env bash

# This tool is intended to build multi-architecture Docker images
#
# PARAMETERS
#
# PLATFORMS
#     Comma-separated list of platforms to build for.
#     Example: "linux/amd64,linux/arm64"
#     Optional. Default: "linux/amd64,linux/arm64".
#
# PUSH_IMAGES
#     If "yes", images will be pushed to registry.
#     Optional. Default: "no".
#
# BUILD_ARGS
#     Build arguments to pass as `docker buildx build --build-arg $BUILD_ARG`
#     Format: BUILD_ARGS="NODE_VERSION=4.x JDK=jdk-8"
#     Optional. Default: None.
#
# TAG
#     Tag name used for the Docker image.
#     Optional. Default: Image version defined in 'version.yml'.
#
# EXTRA_TAGS
#     Additional tags to apply to the Docker image.
#     Format: EXTRA_TAGS="alpha beta latest"
#     Optional. Default: Extra tags defined in 'version.yml'.
#
# TAG_COMMIT
#     If "yes", an extra tag will be added with the current commit.
#     Optional. Default: "yes".
#
# DOCKERFILE
#     Location of Dockerfile to build the image.
#     Optional. Default: "Dockerfile".
#
# IMAGE_NAME
#     Name of image to build.
#     Optional. Default: Image name defined in 'version.yml'.
#
# IMAGE_NAME_PREFIX
#     A prefix to be prepended to the image name.
#     Optional. Default: None.
#
# IMAGE_NAME_SUFFIX
#     A suffix to be appended to the image name.
#     Optional. Default: None.
#
# BUILDER_NAME
#     Name of the buildx builder instance to use.
#     Optional. Default: "kurento-multiarch-builder".



# Shell setup
# -----------

# Absolute Canonical Path to the directory that contains this script.
SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
# shellcheck source=bash.conf.sh
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace

# Avoid illegal chars in tag name
TAG=$(echo $TAG | tr '/' '_')

[[ -z "${PLATFORMS:-}" ]] && PLATFORMS="linux/amd64,linux/arm64"
[[ -z "${PUSH_IMAGES:-}" ]] && PUSH_IMAGES="no"
[[ -z "${TAG_COMMIT:-}" ]] && TAG_COMMIT="yes"
[[ -z "${BUILDER_NAME:-}" ]] && BUILDER_NAME="kurento-multiarch-builder"

if [[ -n "${DOCKERFILE:-}" ]]; then
    FOLDER="$(dirname "$DOCKERFILE")"
else
    FOLDER="$PWD"
fi

# shellcheck source=parse_yaml.sh
. "$SELF_DIR/parse_yaml.sh"
eval $(parse_yaml "$FOLDER/version.yml" "")
commit="$(git rev-parse --short HEAD)"

[[ -z "${DOCKERFILE:-}" ]] && DOCKERFILE="Dockerfile"
[[ -z "${IMAGE_NAME:-}" ]] && IMAGE_NAME="${image_name:-}"
[[ -z "${IMAGE_NAMESPACE:-}" ]] && IMAGE_NAMESPACE="${image_namespace:-}"
[[ -z "${IMAGE_AUTHORS:-}" ]] && IMAGE_AUTHORS="${image_authors:-}"
[[ -z "${TAG:-}" ]] && TAG="${image_version:-}"
[[ -z "${EXTRA_TAGS:-}" ]] && EXTRA_TAGS="${image_extra_tags[*]}"

IMAGE_NAME="${IMAGE_NAME_PREFIX:-}${IMAGE_NAME}${IMAGE_NAME_SUFFIX:-}"

# Setup buildx builder
log "Setting up Docker buildx builder: $BUILDER_NAME"
if ! docker buildx inspect "$BUILDER_NAME" >/dev/null 2>&1; then
    log "Creating new buildx builder: $BUILDER_NAME"
    docker buildx create --name "$BUILDER_NAME" --driver docker-container --use || {
        log "ERROR: Failed to create buildx builder"
        exit 1
    }
else
    log "Using existing buildx builder: $BUILDER_NAME"
    docker buildx use "$BUILDER_NAME"
fi

# Bootstrap the builder (pulls buildkit image if needed)
docker buildx inspect --bootstrap

# Build args
build_args=()
for BUILD_ARG in ${BUILD_ARGS:-}; do
    build_args+=(--build-arg "$BUILD_ARG")
done

# Collect all tags
all_tags=()
all_tags+=("${IMAGE_NAME}:${TAG}")

if [[ "$TAG_COMMIT" == "yes" ]]; then
    all_tags+=("${IMAGE_NAME}:${TAG}-${commit}")
fi

log "Extra tags: $EXTRA_TAGS"
for EXTRA_TAG in ${EXTRA_TAGS}; do
    all_tags+=("${IMAGE_NAME}:${EXTRA_TAG}")
done

# Build tag arguments
tag_args=()
for tag in "${all_tags[@]}"; do
    tag_args+=(--tag "$tag")
done

# Docker login if pushing
if [[ "$PUSH_IMAGES" == "yes" ]]; then
    log "Logging in to Docker Hub"
    docker login -u "$KURENTO_DOCKERHUB_USERNAME" -p "$KURENTO_DOCKERHUB_TOKEN" || {
        log "ERROR: Docker login failed"
        exit 1
    }
fi

# Build command
build_cmd=(
    docker buildx build
    --platform "$PLATFORMS"
    --pull
    --rm
    "${build_args[@]}"
    "${tag_args[@]}"
    -f "$DOCKERFILE"
)

# Add --push or --load based on PUSH_IMAGES
if [[ "$PUSH_IMAGES" == "yes" ]]; then
    build_cmd+=(--push)
else
    # For local builds, we can't use --load with multi-platform
    # Images will be stored in the build cache
    log "WARNING: Multi-platform builds without --push only store in build cache"
    log "To inspect images locally, build for single platform with --load"
fi

build_cmd+=("$FOLDER")

# Execute build
log "Building multi-arch image for platforms: $PLATFORMS"
log "Command: ${build_cmd[*]}"

"${build_cmd[@]}" || {
    log "ERROR: Command failed: docker buildx build"
    exit 1
}

log "### BUILT IMAGES (in build cache)"
docker buildx imagetools inspect "${IMAGE_NAME}:${TAG}" 2>/dev/null || log "Images stored in build cache only"

# Logout
if [[ "$PUSH_IMAGES" == "yes" ]]; then
    docker logout
fi

log "==================== END ===================="
