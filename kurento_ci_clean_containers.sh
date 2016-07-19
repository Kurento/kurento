#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_clean_containers #####################"

# This tool uses a set of variables expected to be exported by tester
# WORKSPACE path
#    Mandatory
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.

# Run clean this way to make sure script is not interrupted
# if post build is aborted (click more than once in X)

if [ -n $WORKSPACE ]; then
  echo "Making all files in $WORKSPACE owned by jenkins"
  sudo chown -R jenkins:jenkins $WORKSPACE
else
  echo "No WORKSPACE env variable defined"
fi

# Make jenkins owner of all files in workspace to avoid https://issues.jenkins-ci.org/browse/JENKINS-24824

cat > clean.sh <<EOF
#!/bin/bash -x
for CONTAINER in \$(docker ps -a |grep $BUILD_TAG | awk '{print \$1}') ; do
   docker stop \$CONTAINER
   docker rm -v -f \$CONTAINER
done
EOF
chmod 755 ./clean.sh
nohup ./clean.sh &
