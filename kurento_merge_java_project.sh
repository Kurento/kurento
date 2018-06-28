#!/bin/bash -x

echo "##################### EXECUTE: kurento_merge_java_project #####################"

# MAVEN_KURENTO_SNAPSHOTS url
#   URL of Kurento repository for maven snapshots
#
# MAVEN_KURENTO_RELEASES url
#   URL of Kurento repository for maven releases
#
# MAVEN_SONATYPE_NEXUS_STAGING url
#   URL of Central staging repositories.
#   Pass it empty to avoid deploying to nexus (private projects):
#   export MAVEN_SONATYPE_NEXUS_STAGING=
#   kurento_merge_java_project.sh

PATH=$PATH:${KURENTO_SCRIPTS_HOME}

echo "[kurento_merge_java_project] Command: kurento_check_version (tagging disabled)"
kurento_check_version.sh false || {
  echo "[kurento_merge_java_project] ERROR: Command failed: kurento_check_version (tagging disabled)"
  exit 1
}

# Deploy to Kurento repositories
export SNAPSHOT_REPOSITORY=$MAVEN_S3_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_S3_KURENTO_RELEASES
echo "[kurento_merge_java_project] Command: kurento_maven_deploy (Kurento)"
kurento_maven_deploy.sh || {
  echo "[kurento_merge_java_project] ERROR: Command failed: kurento_maven_deploy (Kurento)"
  exit 1
}

# Deploy to Maven Central (only release)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
echo "[kurento_merge_java_project] Command: kurento_maven_deploy (Sonatype)"
kurento_maven_deploy.sh || {
  echo "[kurento_merge_java_project] ERROR: Command failed: kurento_maven_deploy (Sonatype)"
  exit 1
}

# Deploy to Kurento Builds only when it is release
VERSION="$(kurento_get_version.sh)" || {
  echo "[kurento_merge_java_project] ERROR: Command failed: kurento_get_version"
  exit 1
}
if [[ $VERSION != *-SNAPSHOT ]]; then
  echo "[kurento_merge_java_project] Version is RELEASE: HTTP publish"

  if [[ -n "$FILES" ]]; then
    echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > project.version
    echo "[kurento_merge_java_project] Command: kurento_http_publish"
    FILES=$FILES kurento_http_publish.sh
  else
    echo "[kurento_merge_java_project] No FILES provided, skip HTTP publish"
  fi
else
  echo "[kurento_merge_java_project] Version is SNAPSHOT, skip HTTP publish"
fi

# Only create a tag if the deployment process was successful
echo "[kurento_merge_java_project] Command: kurento_check_version (tagging enabled)"
kurento_check_version.sh true || {
  echo "[kurento_merge_java_project] ERROR: Command failed: kurento_check_version (tagging enabled)"
  exit 1
}

echo "[kurento_merge_java_project] Done"
