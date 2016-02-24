#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_dnat_hook_handler #####################"

exec > hook.out
exec 2>&1

echo "Arguments: $*"

event=$1
container=$2

echo "Event:|$event| Container:|$container|"

if [ $event = 'start' ]; then
  echo "start event"
  inspect=$(docker inspect $container|grep "\"KurentoDnat\": \"true\"")
  if [ $? = 0 ]; then
    echo "Starting container $container with dnat label. Preparing dnat."
    touch $container.id
  fi
fi

if [ $event = 'destroy' ]; then
  echo "Destroying container $container."
  if [ -f $container.id ]; then
    echo "Container with dnat found. Deleting dnat rules."
    rm $container.id
  else
    echo "Container not found. Ignoring."
  fi
fi
