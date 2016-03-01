#!/bin/bash -x

for nsname in $(ip netns list|grep -e '-cont'); do
  ip netns del $nsname
done

for nsname in $(ip netns list|grep -e '-route'); do
  ip netns del $nsname
done

for nsname in $(ip netns list|grep -e '-bridge'); do
  ip netns del $nsname
done

for veth in $(ip link list | grep vethci | awk '{print $1}' | cut -f1 -d:); do
  vethpid=$(echo $veth | grep -Po '\d+')
  ip netns exec $vethpid-cont ip link del $veth
done

for veth in $(ip link list | grep vethce | awk '{print $1}' | cut -f1 -d:); do
  vethpid=$(echo $veth | grep -Po '\d+')
  ip netns exec $vethpid-bridge ip link del $veth
done

for veth in $(ip link list | grep vethrbe | awk '{print $1}' | cut -f1 -d:); do
  vethpid=$(echo $veth | grep -Po '\d+')
  ip netns exec $vethpid-bridge ip link del $veth
done

for veth in $(ip link list | grep vethrbi | awk '{print $1}' | cut -f1 -d:); do
  vethpid=$(echo $veth | grep -Po '\d+')
  ip netns exec $vethpid-route ip link del $veth
done

for veth in $(ip link list | grep vethrai | awk '{print $1}' | cut -f1 -d:); do
  vethpid=$(echo $veth | grep -Po '\d+')
  ip netns exec $vethpid-route ip link del $veth
done

for vethrae in $(ip link list | grep vethrae | awk '{print $1}' | cut -f1 -d:); do
  ip link del $vethrae
done
