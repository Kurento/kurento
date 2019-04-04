#!/usr/bin/env bash

echo "##################### EXECUTE: kurento_ci_container_entrypoint #####################"

# Enable command traces for debugging
set -o xtrace

#/
#/ Arguments:
#/
#/   All: Commands to run.
#/      Mandatory.
#/

RUN_COMMANDS=("$@")
[ -z "${RUN_COMMANDS:+x}" ] && {
    echo "[kurento_ci_container_entrypoint] ERROR: Missing argument(s): Commands to run"
    exit 1
}

echo "[kurento_ci_container_entrypoint] Preparing environment..."

# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${BASEPATH}/kms:${PATH}"

DIST=$(lsb_release -c)
DIST=$(echo ${DIST##*:} | tr -d ' ' | tr -d '\t')
export DEBIAN_FRONTEND=noninteractive

echo "[kurento_ci_container_entrypoint] Show environment..."
env | sort

# Configure SSH keys
if [ -f "$GIT_KEY" ]; then
    mkdir -p /root/.ssh
    cp $GIT_KEY /root/.ssh/git_id_rsa
    chmod 600 /root/.ssh/git_id_rsa
    export KEY=/root/.ssh/git_id_rsa
    cat >> /root/.ssh/config <<-EOF
      StrictHostKeyChecking no
      User $([ -n "$GERRIT_USER" ] && echo $GERRIT_USER || echo jenkinskurento)
      IdentityFile /root/.ssh/git_id_rsa
EOF
    if [ "$DIST" = "xenial" ]; then
      cat >> /root/.ssh/config<<-EOF
        KexAlgorithms +diffie-hellman-group1-sha1
EOF
    fi
fi

if [ -n "$UBUNTU_PRIV_S3_ACCESS_KEY_ID" ] && [ -n "$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID" ]; then
  echo "AccessKeyId = $UBUNTU_PRIV_S3_ACCESS_KEY_ID
  SecretAccessKey = $UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID
  Token = ''" >/etc/apt/s3auth.conf
fi

wget http://archive.ubuntu.com/ubuntu/pool/main/libt/libtimedate-perl/libtimedate-perl_2.3000-2_all.deb
dpkg -i libtimedate*deb
rm libtimedate*deb

wget -O - http://ubuntu.kurento.org/kurento.gpg.key | apt-key add -
apt-get update

# Configure Kurento gnupg
if [ -f "$GNUPG_KEY" ]; then
  echo "GNUPGHOME=$GNUPGHOME"
  echo "HOME=$HOME"
  gpg --homedir "$HOME/.gnupg" --import "$GNUPG_KEY"
fi

# For backwards compatibility with kurento_clone_repo / Update to use github instead of gerrit
export KURENTO_GIT_REPOSITORY=${KURENTO_GIT_REPOSITORY}

# Configure private bower Repository
cat >/root/.bowerrc << EOL
{
   "registry": "http://bower.kurento.org:5678" ,
   "strict-ssl": false
}
EOL

CODE_KURENTO_ORG=$(getent hosts code.kurento.org | awk '{ print $1 }')
while [ -z $CODE_KURENTO_ORG ]
do
  CODE_KURENTO_ORG=$(getent hosts code.kurento.org | awk '{ print $1 }')
done

MAVEN_KURENTO_ORG=$(getent hosts maven.kurento.org | awk '{ print $1 }')
while [ -z $MAVEN_KURENTO_ORG ]
do
  MAVEN_KURENTO_ORG=$(getent hosts maven.kurento.org | awk '{ print $1 }')
done

BOWER_KURENTO_ORG=$(getent hosts bower.kurento.org | awk '{ print $1 }')
while [ -z $BOWER_KURENTO_ORG ]
do
  BOWER_KURENTO_ORG=$(getent hosts bower.kurento.org | awk '{ print $1 }')
done

echo "$CODE_KURENTO_ORG code.kurento.org" >> /etc/hosts
echo "$MAVEN_KURENTO_ORG maven.kurento.org" >> /etc/hosts
echo "$BOWER_KURENTO_ORG bower.kurento.org" >> /etc/hosts

echo "[kurento_ci_container_entrypoint] Network configuration"
ip addr list

# Use the Openvidu package proxy for all apt-get commands
# The proxy is HTTP-only, so other repos should be accessed directly
echo 'Acquire::http::Proxy "http://proxy.openvidu.io:3142";'  >/etc/apt/apt.conf.d/01proxy
echo 'Acquire::http::Proxy::deb.nodesource.com "DIRECT";'    >>/etc/apt/apt.conf.d/01proxy

for COMMAND in "${RUN_COMMANDS[@]}"; do
    echo "[kurento_ci_container_entrypoint] Run command: '$COMMAND'"
    eval $COMMAND || {
        echo "[kurento_ci_container_entrypoint] ERROR: Command failed: '$COMMAND'"
        exit 1
    }
done
