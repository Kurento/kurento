#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_entrypoint #####################"

[ -n "$1" ] || { echo "No script to run specified. Need one to run after preparing the environment"; exit 1; }
BUILD_COMMAND=$@

PATH=$PATH:$(realpath $(dirname "$0"))

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
  # For compatibility with kurento_generate_debian_package:
  export KEY_ID=$GNUPG_KEY
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

for CMD in $BUILD_COMMAND; do
  echo "Running command: $CMD"
  $CMD || exit 1
done
