#!/usr/bin/env bash

# This tool uses a set of variables expected to be exported by tester
# WORKSPACE path
#    Mandatory
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#
# BUILD_TAG string
#    Mandatory
#    A name to uniquely identify containers created within this job
#
# DOCKER_IMAGE
#    Mandatory
#    Docker image to run
#
# COMMAND
#    Mandatory
#    Command to run. The command is run with bash -c "$COMMAND"
#
# MAVEN_SETTINGS path
#    Mandatory
#    Location of the settings.xml file used by maven
#
# MAVEN_KURENTO_RELEASES
#    Optional
#
# MAVEN_KURENTO_SNAPSHOTS
#    Optional
#
# MAVEN_SONATYPE_NEXUS_STAGING
#    Optional
#
# KURENTO_GIT_REPOSITORY
#    Optional
#



# NOTE: Other scripts expect that this script only prints the command output.
# Don't print any debug messages, and if you do, make sure they are redirected
# to stderr.
#
# For this reason, we don't load our shell setup script here: bash.conf.sh

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



cat >"$PWD/.root-config" <<EOL
StrictHostKeyChecking no
User jenkins
IdentityFile /root/.ssh/id_rsa
EOL

docker run \
    --name "$BUILD_TAG" \
    --rm \
    -v "$WORKSPACE":/opt/kurento \
    -v "$KURENTO_SCRIPTS_HOME":/opt/adm-scripts \
    -v "$HOME/.ssh":/root/.ssh:ro \
    -v "$HOME/.gitconfig":/root/.gitconfig \
    -v "$PWD/.root-config":/root/.ssh/config \
    -v "$MAVEN_SETTINGS":/opt/kurento-settings.xml \
    -e "MAVEN_KURENTO_RELEASES=${MAVEN_KURENTO_RELEASES:-}" \
    -e "MAVEN_KURENTO_SNAPSHOTS=${MAVEN_KURENTO_SNAPSHOTS:-}" \
    -e "MAVEN_SONATYPE_NEXUS_STAGING=${MAVEN_SONATYPE_NEXUS_STAGING:-}" \
    -e "KURENTO_GIT_REPOSITORY=${KURENTO_GIT_REPOSITORY:-}" \
    -e "MAVEN_SETTINGS=/opt/kurento-settings.xml" \
    -w /opt/kurento \
    --entrypoint /bin/bash \
    "$DOCKER_IMAGE" \
    -x -c "$COMMAND"
