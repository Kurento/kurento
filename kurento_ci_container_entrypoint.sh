#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_entrypoint #####################"

[ -n "$1" ] || { echo "No script to run specified. Need one to run after preparing the environment"; exit 1; }
BUILD_COMMAND=$@

PATH=$PATH:$(realpath $(dirname "$0")):$(realpath $(dirname "$0"))/kms

echo "Preparing environment..."

# Configure SSH keys
if [ -f "$GIT_KEY" ]; then
    mkdir -p /root/.ssh
    cp $GIT_KEY /root/.ssh/git_id_rsa
    chmod 600 /root/.ssh/git_id_rsa
    export KEY=/root/.ssh/git_id_rsa
    cat >> /root/.ssh/config <<-EOF
      StrictHostKeyChecking no
      User $([ -n "$GERRIT_USER" ] && echo $GERRIT_USER || echo jenkins)
      IdentityFile /root/.ssh/git_id_rsa
EOF
fi

# Configure Kurento gnupg
if [ -f "$GNUPG_KEY" ]; then
  gpg --import $GNUPG_KEY
fi

# For backwards compatibility with kurento_clone_repo
export KURENTO_GIT_REPOSITORY=ssh://$GERRIT_USER@$GERRIT_HOST:$GERRIT_PORT

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

CHROMEDRIVER_KURENTO_ORG=$(getent hosts chromedriver.kurento.org | awk '{ print $1 }')
while [ -z $CHROMEDRIVER_KURENTO_ORG ]
do
  CHROMEDRIVER_KURENTO_ORG=$(getent hosts chromedriver.kurento.org | awk '{ print $1 }')
done

BOWER_KURENTO_ORG=$(getent hosts bower.kurento.org | awk '{ print $1 }')
while [ -z $BOWER_KURENTO_ORG ]
do
  BOWER_KURENTO_ORG=$(getent hosts bower.kurento.org | awk '{ print $1 }')
done

echo "$CODE_KURENTO_ORG code.kurento.org" >> /etc/hosts
echo "$MAVEN_KURENTO_ORG maven.kurento.org" >> /etc/hosts
echo "$CHROMEDRIVER_KURENTO_ORG chromedriver.kurento.org" >> /etc/hosts
echo "$BOWER_KURENTO_ORG bower.kurento.org" >> /etc/hosts

echo "Network configuration"
ip addr list

for CMD in $BUILD_COMMAND; do
  echo "Running command: $CMD"
  $CMD || exit 1
done
