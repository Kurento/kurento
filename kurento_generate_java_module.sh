#!/bin/bash -x

echo "##################### EXECUTE: kurento_generate_java_module #####################"

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

kurento_check_version.sh false || {
  echo "[kurento_generate_java_module] ERROR: Command failed: kurento_check_version (tagging disabled)"
  exit 1
}

rm -rf build
mkdir build && cd build
cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE || {
  echo "[kurento_generate_java_module] ERROR: Command failed: cmake"
  exit 1
}

cd java || {
  echo "[kurento_generate_java_module] ERROR: Expected directory doesn't exist: java/"
  exit 1
}

# Deploy to Kurento repositories
export SNAPSHOT_REPOSITORY=$MAVEN_S3_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_S3_KURENTO_RELEASES
kurento_maven_deploy.sh || {
  echo "[kurento_generate_java_module] ERROR: Command failed: kurento_maven_deploy (Kurento)"
  exit 1
}

# Deploy to Maven Central (only release)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh || {
  echo "[kurento_generate_java_module] ERROR: Command failed: kurento_maven_deploy (Sonatype)"
  exit 1
}

# Only create a tag if the deployment process was successful
# kurento_check_version.sh true || {
#   echo "[kurento_generate_java_module] ERROR: Command failed: kurento_check_version (tagging enabled)"
#   exit 1
# }
