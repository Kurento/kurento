#!/usr/bin/env bash

#/ CI job - Deploy AWS CloudFormation templates to Amazon S3.
#/
#/ This script is meant to be called from the "Execute shell" section of a
#/ Jenkins job that wants to deploy AWS CloudFormation templates.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from job parameters (with "This project is parameterized"):
#/
#/ JOB_RELEASE
#/
#/   "true" for release versions. "false" for nightly snapshot builds.
#/
#/
#/ * Variable(s) from job Custom Tools (with "Install custom tools"):
#/
#/ OPENVIDU_ADM_SCRIPTS_HOME
#/
#/   Jenkins path to 'adm-scripts', containing all OpenVidu CI scripts.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Check optional parameters
if [[ "$JOB_RELEASE" != "true" ]]; then
    log "Not a release buid; nothing to do"
    exit 0
fi

# Get version number for the deployment
# shellcheck disable=SC2012
KMS_DEB_FILE="$(ls -v -1 kurento-media-server_*.deb | tail -n 1)"
if [[ -z "$KMS_DEB_FILE" ]]; then
    log "ERROR: Cannot find KMS package file: kurento-media-server_*.deb"
    exit 1
fi
KMS_VERSION="$(
    dpkg --field "$KMS_DEB_FILE" Version \
        | grep --perl-regexp --only-matching '^(\d+\.\d+\.\d+)'
)"
if [[ -z "$KMS_VERSION" ]]; then
    log "ERROR: Cannot parse KMS Version field"
    exit 1
fi

# Build the AWS CloudFormation template
AWS_CF_TEMPLATE_FILE="KMS-Coturn-cfn-${KMS_VERSION}.yaml"

cat >run.sh <<EOF
#!/usr/bin/env bash

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

cd ./AWS/
sed "s/RELEASE/$KMS_VERSION/" KMS-Coturn-cfn.yaml.template \
    >"$AWS_CF_TEMPLATE_FILE"
aws s3 cp "$AWS_CF_TEMPLATE_FILE" "s3://aws.kurento.org" --acl public-read
EOF
chmod +x run.sh

export CONTAINER_IMAGE="openvidu/openvidu-dev-generic:0.1"
export BUILD_COMMAND="./run.sh"
"${OPENVIDU_ADM_SCRIPTS_HOME}/openvidu_ci_container_job_setup.sh"

log "New AWS CloudFormation template deployed: '$AWS_CF_TEMPLATE_FILE'"
