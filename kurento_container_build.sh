#!/bin/bash -x

# This tool is intended to build Docker images
#
# PARAMETERS
#
# PUSH_IMAGES
#     Optional
#
#     True if images should be pushed to registry
#     Either absent or any other value is considered false
#
# DOCKERFILE
#     Optional
#     Location of Dockerfile to build

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
[ -n "$TAG" ] || TAG=$image_version
echo "Extra tags: ${image_extra_tags[@]}"
[ -n "$EXTRA_TAGS" ] || EXTRA_TAGS="${image_extra_tags[@]}"

# Build using a tag composed of the original tag and the short commit id
docker build --no-cache --rm=true -t $IMAGE_NAME:${TAG}-${commit} -f $DOCKERFILE

# Tag the resulting image using the original tag
docker tag -f $IMAGE_NAME:$TAG $IMAGE_NAME:$TAG

# Apply any additional tags required
echo "Extra tags: $EXTRA_TAGS"
for EXTRA_TAG in $EXTRA_TAGS
do
  docker tag -f $IMAGE_NAME:$TAG $IMAGE_NAME:$EXTRA_TAG
done

echo "### DOCKER IMAGES"
docker images

echo "#### SPACE AVAILABLE"
df -h

# Push
if [ "$PUSH_IMAGES" = "yes" ]; then
  docker login -u "$KURENTO_REGISTRY_USER" -p "$KURENTO_REGISTRY_PASSWD" -e "$KURENTO_EMAIL" $KURENTO_REGISTRY_URI
  docker push $KURENTO_REGISTRY_URI/$IMAGE_NAME:$TAG
  dogestry push s3://kurento-docker/?region=eu-west-1 $IMAGE_NAME:$TAG

  for $EXTRA_TAG in $EXTRA_TAGS
  do
    docker push $KURENTO_REGISTRY_URI/$IMAGE_NAME:$EXTRA_TAG
    dogestry push s3://kurento-docker/?region=eu-west-1 $IMAGE_NAME:$EXTRA_TAG
  done

  docker logout
fi
