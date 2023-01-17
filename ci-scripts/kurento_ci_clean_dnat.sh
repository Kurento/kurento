#!/bin/bash -x

echo "**************************** kurento_ci_clean_dnat ******************************************"

if [ -n $1 -a $1 = 'all' ]; then

  echo "Removing all network resources"

  for nsname in $(sudo ip netns list|grep -e '-cont'); do
    sudo ip netns del $nsname
  done

  for nsname in $(sudo ip netns list|grep -e '-route'); do
    sudo ip netns del $nsname
  done

  for nsname in $(sudo ip netns list|grep -e '-bridge'); do
    sudo ip netns del $nsname
  done

  for veth in $(sudo ip link list | grep vethci | awk '{print $1}' | cut -f1 -d:); do
    vethpid=$(echo $veth | grep -Po '\d+')
    sudo ip netns exec $vethpid-cont ip link del $veth
  done

  for veth in $(sudo ip link list | grep vethce | awk '{print $1}' | cut -f1 -d:); do
    vethpid=$(echo $veth | grep -Po '\d+')
    sudo ip netns exec $vethpid-bridge ip link del $veth
  done

  for veth in $(sudo ip link list | grep vethrbe | awk '{print $1}' | cut -f1 -d:); do
    vethpid=$(echo $veth | grep -Po '\d+')
    sudo ip netns exec $vethpid-bridge ip link del $veth
  done

  for veth in $(sudo ip link list | grep vethrbi | awk '{print $1}' | cut -f1 -d:); do
    vethpid=$(echo $veth | grep -Po '\d+')
    sudo ip netns exec $vethpid-route ip link del $veth
  done

  for veth in $(sudo ip link list | grep vethrai | awk '{print $1}' | cut -f1 -d:); do
    vethpid=$(echo $veth | grep -Po '\d+')
    sudo ip netns exec $vethpid-route ip link del $veth
  done

  for vethrae in $(sudo ip link list | grep vethrae | awk '{print $1}' | cut -f1 -d:); do
    sudo ip link del $vethrae
  done

else
  for cidfile in $(ls *.id); do

    cid=$(cat $cidfile)

    echo "PID file: $cidfile. Removing resources associated to container PID $cid"

    sudo ip netns exec $cid-cont ip link del vethci$cid
    sudo ip netns exec $cid-bridge ip link del vethce$cid
    sudo ip netns exec $cid-bridge ip link del vethrbe$cid
    sudo ip netns exec $cid-route ip link del vethrbi$cid
    sudo ip netns exec $cid-route ip link del vethrai$cid
    sudo ip link del vethrae$cid
    sudo ip netns del $cid-cont
    sudo ip netns del $cid-route
    sudo ip netns del $cid-bridge

  done
fi
