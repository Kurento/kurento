#!/bin/bash -x

# This tool is intended to build Docker images
#
# PARAMETERS
#
# PUSH_IMAGES
#     Optional
#
#     yes if images should be pushed to registry
#     Either absent or any other value is considered false
#
# BUILD_ARGS
#     Optional
#     Build arguments to pass to docker build.
#     Format: NODE_VERSION=4.x JDK=jdk-8
#
# DOCKERFILE
#     Optional
#     Location of Dockerfile to build
#
# IMAGE_NAME
#     Optional
#     Name of image to build
#
# EXTRA_TAGS
#     Optional
#     Extra tags to apply to image


[ -n $PUSH_IMAGES ] || PUSH_IMAGES='no'

if [ "x$DOCKERFILE" != "x" ]; then
  FOLDER=$(dirname $DOCKERFILE)
else
  FOLDER=$PWD
fi

. parse_yaml.sh
eval $(parse_yaml $FOLDER/version.yml "")
commit=$(git rev-parse --short HEAD)

[ -n "$DOCKERFILE" ] || DOCKERFILE=./Dockerfile
[ -n "$IMAGE_NAME" ] || IMAGE_NAME=$image_name
[ -n "$IMAGE_NAMESPACE" ] || IMAGE_NAMESPACE=$image_namespace
[ -n "$IMAGE_AUTHORS" ] || IMAGE_AUTHORS=$image_authors
[ -n "$TAG" ] || TAG=$image_version
echo "Extra tags: ${image_extra_tags[@]}"
[ -n "$EXTRA_TAGS" ] || EXTRA_TAGS="${image_extra_tags[@]}"

IMAGE=$(echo $IMAGE_NAME | cut -d/ -f2)

# If there's a generate.sh script, assume we need to dynamically generate the Dockerfile using it
# This is the case of selenium images
if [ -f generate.sh ]; then
  echo "Generating Dockerfile..."
  [ -n "${image_parent_version}" ] || image_parent_version=$TAG
  [ -n "${image_namespace}" ] || image_namespace="kurento"
  [ -n "${image_authors}" ] || image_authors="Kurento Team"
  ./generate.sh ${image_parent_version} ${image_namespace} ${image_authors}
fi

# If there's a kurento-generate.sh script, assume we need to fix the FROM line inside the Dockerfie
# in order to use our own generates Docker Images
if [ -f kurento-generate.sh ]; then
  echo "Applying Kurento customization..."
  if [[ $FOLDER == *"Debug"* ]]; then
    ./kurento-generate.sh ${image_parent_version} ${image_namespace} ${image_authors}
  else
    ./kurento-generate.sh
  fi
fi

# Build using a tag composed of the original tag and the short commit id
for BUILD_ARG in $BUILD_ARGS
do
  build_args+=("--build-arg $BUILD_ARG")
done
docker build --no-cache --rm=true ${build_args[@]} -t $IMAGE -f $DOCKERFILE $FOLDER || exit 1

# Tag the resulting image using the original tag
docker tag $IMAGE $IMAGE_NAME:$TAG
docker tag $IMAGE $IMAGE_NAME:$TAG-${commit}

# Apply any additional tags required
echo "Extra tags: $EXTRA_TAGS"
for EXTRA_TAG in $EXTRA_TAGS
do
  docker tag $IMAGE $IMAGE_NAME:$EXTRA_TAG
done

echo "### DOCKER IMAGES"
docker images | grep $IMAGE_NAME

echo "#### SPACE AVAILABLE"
df -h

# Push
if [ "$PUSH_IMAGES" == "yes" ]; then
  docker login -u "$KURENTO_DOCKERHUB_USER" -p "$KURENTO_DOCKERHUB_PASSWD" -e "$KURENTO_EMAIL"
  #docker tag $IMAGE:${TAG}-${commit} $IMAGE_NAME:${TAG}-${commit}
  docker push $IMAGE_NAME:${TAG}-${commit}

  #docker tag $IMAGE:${TAG}-${commit} $IMAGE_NAME:$TAG
  docker push $IMAGE_NAME:$TAG

  for EXTRA_TAG in $EXTRA_TAGS
  do
    #docker tag $IMAGE:$TAG $IMAGE_NAME:$EXTRA_TAG
    docker push $IMAGE_NAME:$EXTRA_TAG
  done

  docker logout
fi

# Remove dangling images
if [ $(docker images -f "dangling=true" -q | wc -l) -ne 0 ]; then
  docker rmi $(docker images -f "dangling=true" -q) || exit 0
fi
