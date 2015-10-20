#!/bin/bash

# Run clean this way to make sure script is not interrupted
# if post build is aborted (click more than once in X)
cat > clean.sh <<EOF
#!/bin/bash -x
for CONTAINER in \$(docker ps -a |grep $BUILD_TAG | awk '{print \$1}') ; do
   docker stop \$CONTAINER
   docker rm -v \$CONTAINER
done
EOF
chmod 755 ./clean.sh
nohup ./clean.sh &
