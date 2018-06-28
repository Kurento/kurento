#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_dnat_hook_handler #####################"

# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${PATH}"

exec >> hook.log
exec 2>&1

echo "Arguments: $*"

event=$1
container=$2

echo "Event:|$event| Container:|$container|"

if [ $event = 'start' ]; then

  # Check if this container has been started by our job
  name=$(docker inspect -f '{{.Name}}' $container)
  if [[ ! ${name:1} == ${BUILD_TAG}* ]]; then
    echo "It's not my container"
    exit 0
  fi

  docker inspect $container
  inspect=$(docker inspect -f '{{.Config.Labels.KurentoDnat}}' $container)
  if [[ ! $inspect == 'true' ]]; then
    echo "It's not a dnat container. Skip."
    exit 0
  fi

  echo "[$container] **** Starting container $name with dnat label. Preparing dnat."
  #Check ip
  ip=$(docker inspect -f '{{.Config.Labels.IpAddress}}' $container)

  # Check transport
  transport=$(docker inspect -f '{{.Config.Labels.Transport}}' $container)

  docker_pid=$(docker inspect -f '{{.State.Pid}}' $container)
  echo $docker_pid > $container.id
  echo "[$container] >>>> Calling dnat script"
  sudo "${BASEPATH}/kurento_ci_container_dnat.sh" $container $event $docker_pid $transport $ip >> dnat2.log
fi

if [ $event = 'stop' ]; then
  echo "[$container] ++++ Stopping container $name with id $container"
fi

if [ $event = 'destroy' ]; then
  echo "[$container] ---- Destroying container $name with id $container"
  if [ -f $container.id ]; then
    echo "Container with dnat found. Deleting dnat rules."
    docker_pid=$(cat $container.id)
    echo "Calling dnat script"
    sudo "${BASEPATH}/kurento_ci_container_dnat.sh" $container $event $docker_pid >> dnat2destroy.log
  else
    echo "Container not found. Ignoring."
  fi
fi

if [ $event == 'die' ]; then
  echo "[$container] ???? Dying container $name with id $container"
fi
