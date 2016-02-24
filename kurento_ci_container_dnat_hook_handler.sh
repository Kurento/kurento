#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_dnat_hook_handler #####################"

PATH=$PATH:$(realpath $(dirname "$0"))

exec >> hook.log
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
    # Check transport
    result=$(docker inspect $container|grep "\"Transport\": \"TCP\"")
    if [ $? = 0 ]; then
      transport="tcp"
    else
      transport="udp"
    fi
    touch $container.id
    kurento_ci_container_dnat.sh $container $event $transport
  fi
fi

if [ $event = 'destroy' ]; then
  echo "Destroying container $container."
  if [ -f $container.id ]; then
    echo "Container with dnat found. Deleting dnat rules."
    rm $container.id
    kurento_ci_container_dnat.sh $container $event $transport
  else
    echo "Container not found. Ignoring."
  fi
fi
