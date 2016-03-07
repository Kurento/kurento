#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_dnat_hook_handler #####################"

PATH=$PATH:$(realpath $(dirname "$0"))

exec >> hook.log
exec 2>&1

echo "Arguments: $*"

event=$1
container=$2

echo "Event:|$event| Container:|$container|"

# Check if this container has been started by our job
name=$(docker inspect -f '{{.Name}}' $container)
if [[ ! ${name:1} == ${BUILD_TAG}* ]]; then
  echo "It's not my container"
  exit 0
fi

if [ $event = 'start' ]; then
  echo "**** Starting container $name"
  docker inspect $container
  inspect=$(docker inspect $container|grep "\"KurentoDnat\": \"true\"")
  if [ $? = 0 ]; then
    echo "Starting container $container with dnat label. Preparing dnat."
    #Check ip
    echo $(docker inspect $container|grep "IpAddress"|awk {'print $2'})
    echo $(docker inspect $container|grep "IpAddress"|awk {'print $2'}|sed 's/"//g')
    echo $(docker inspect $container|grep "IpAddress"|awk {'print $2'}|sed 's/"//g'|cut -f1 -d",")
    ip=$(docker inspect $container|grep "IpAddress"|awk {'print $2'}|sed 's/"//g'|cut -f1 -d",")

    # Check transport
    result=$(docker inspect $container|grep "\"Transport\": \"TCP\"")
    if [ $? = 0 ]; then
      transport="tcp"
    else
      transport="udp"
    fi
    docker_pid=$(docker inspect -f '{{.State.Pid}}' $container)
    echo $docker_pid > $container.id
    echo "Calling dnat script"
    sudo $(realpath $(dirname "$0"))/kurento_ci_container_dnat.sh $container $event $docker_pid $transport $ip >> dnat2.log
  fi
fi

if [ $event = 'stop' ]; then
  echo "++++ Stopping container $name"
fi

if [ $event = 'destroy' ]; then
  echo "---- Destroying container $name."
  if [ -f $container.id ]; then
    echo "Container with dnat found. Deleting dnat rules."
    docker_pid=$(cat $container.id)
    echo "Calling dnat script"
    sudo $(realpath $(dirname "$0"))/kurento_ci_container_dnat.sh $container $event $docker_pid >> dnat2destroy.log
  else
    echo "Container not found. Ignoring."
  fi
fi

if [ $event == 'die' ]; then
  echo "???? Dying container $name"
fi
