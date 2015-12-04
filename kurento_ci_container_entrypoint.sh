#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_entrypoint #####################"

[ -n "$1" ] || { echo "No script to run specified. Need one to run after preparing the environment"; exit 1; }
BUILD_COMMAND=$1

PATH=$PATH:$(realpath $(dirname "$0"))

echo "Preparing environment..."

# Configure SSH keys
if [ -f "$KEY" ]; then
    mkdir -p /root/.ssh
    cp $KEY /root/.ssh/gerrit_id_rsa
    chmod 600 /root/.ssh/gerrit_id_rsa
    export KEY=/root/.ssh/gerrit_id_rsa
    cat >> /root/.ssh/config <<-EOF
      StrictHostKeyChecking no
      User $([ -n "$GERRIT_USER" ] && echo $GERRIT_USER || echo jenkins)
      IdentityFile /root/.ssh/gerrit_id_rsa
EOF
fi

# Configure Kurento gnupg
[ -f "$GNUPG_KEY" ] && gpg --import $GNUPG_KEY

# Configure private bower Repository
cat >/root/.bowerrc << EOL
{
   "registry": "http://bower.kurento.org:5678" ,
   "strict-ssl": false
}
EOL

echo "Running command $BUILD_COMMAND"
$BUILD_COMMAND
