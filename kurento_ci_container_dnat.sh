#!/bin/bash -x

echo "##################### EXECUTE: kurento_ci_container_dnat #####################"

############################################
# Craete container
container=$1
action=$2
docker_pid=$3
[ -n $4 ] && transport=$4
[ -n $5 ] && ip=$5

echo "Performing $action on container ID $container (pid=$docker_pid) with transport $transport"

if [ $action = 'start' ]; then

echo "Starting..."

echo "Selected ip: $ip"

# Add Net namespaces
[ ! -f /proc/$docker_pid/ns/net ] && echo "No such file /proc/$docker_pid/ns/net"
ln -s /proc/$docker_pid/ns/net /var/run/netns/$docker_pid-cont || (echo "No link created"; exit 1)
ls -la /var/run/netns/
ip netns list
ip netns add $docker_pid-bridge
ip netns add $docker_pid-route

# Create bridge & interfaces
ip netns exec $docker_pid-bridge brctl addbr br0
ip link add vethci$docker_pid type veth peer name vethce$docker_pid
ip link add vethrbi$docker_pid type veth peer name vethrbe$docker_pid
ip link add vethrai$docker_pid type veth peer name vethrae$docker_pid

# Assign interfaces to Net namespaces
ip link set vethci$docker_pid netns $docker_pid-cont
ip link set vethce$docker_pid netns $docker_pid-bridge
ip link set vethrbi$docker_pid netns $docker_pid-route
ip link set vethrbe$docker_pid netns $docker_pid-bridge
ip link set vethrai$docker_pid netns $docker_pid-route

# Link interfaces to bridges
brctl addif docker0 vethrae$docker_pid
ip netns exec $docker_pid-bridge brctl addif br0 vethce$docker_pid
ip netns exec $docker_pid-bridge brctl addif br0 vethrbe$docker_pid


# Configure IP addresses

# Container Internal
ip netns exec $docker_pid-cont ip link set dev vethci$docker_pid name eth0
ip netns exec $docker_pid-cont ip link set eth0 up
ip netns exec $docker_pid-cont ip addr add 192.168.0.100/24 dev eth0
ip netns exec $docker_pid-cont ip route add default via 192.168.0.1

# Container external (peer)
ip netns exec $docker_pid-bridge ip link set vethce$docker_pid up

# Bridge
ip netns exec $docker_pid-bridge ip link set br0 up
ip netns exec $docker_pid-bridge ip addr add 192.168.0.254/24 dev br0

# Router bridge internal
ip netns exec $docker_pid-route ip link set vethrbi$docker_pid up
ip netns exec $docker_pid-route ip addr add 192.168.0.1/24 dev vethrbi$docker_pid

# Router bridge external (peer)
ip netns exec $docker_pid-bridge ip link set vethrbe$docker_pid up

# Agent internal
ip netns exec $docker_pid-route ip link set vethrai$docker_pid up
ip netns exec $docker_pid-route ip addr add ${ip}/16 dev vethrai$docker_pid
ip netns exec $docker_pid-route ip route add default via 172.17.0.1

# Agent external
ip link set vethrae$docker_pid up

# Add SNAT
ip netns exec $docker_pid-route iptables -t nat -A POSTROUTING -o vethrai$docker_pid -j SNAT --to $ip
ip netns exec $docker_pid-route iptables -t nat -A PREROUTING -i vethrai$docker_pid -j DNAT --to 192.168.0.100
if [ $transport = 'TCP' ]; then
  # Allow UDP on port 3478 (for TURN)
  ip netns exec $docker_pid-route iptables -I FORWARD 1 -p udp --dport 3478 -s 192.168.0.0/24 -j ACCEPT
  ip netns exec $docker_pid-route iptables -A FORWARD -p udp --sport 3478 -j ACCEPT

  # Allow DNS
  ip netns exec $docker_pid-route iptables -A FORWARD -p udp --dport 53 -j ACCEPT
  ip netns exec $docker_pid-route iptables -A FORWARD -p udp --sport 53 -j ACCEPT

  ip netns exec $docker_pid-route iptables -A FORWARD -p udp -s 192.168.0.0/24 -j DROP

  # This is used to force RLFX TCP
#  ip netns exec $docker_pid-route iptables -I INPUT 1 -p udp -s 172.17.0.0/16 -j DROP
#  ip netns exec $docker_pid-route iptables -I FORWARD 1 -p udp -s 193.147.0.0/16 --sport 3478 -j ACCEPT
  ip netns exec $docker_pid-route iptables -A FORWARD -p udp -s 172.17.0.0/16 -j DROP

  # This is used to force RELAY
#  ip netns exec $docker_pid-route iptables -I INPUT 1 -p udp -s 172.16.0.0/16 -j DROP
#  ip netns exec $docker_pid-route iptables -I FORWARD 1 -p udp -s 172.16.0.0/16 -j DROP
  ip netns exec $docker_pid-route iptables -A FORWARD -p udp -s 193.147.0.0/16 -j DROP
fi

fi

if [ $action = 'destroy' ]; then

  ######################################################
  # Delete container
  echo "Destroying netns & interfaces related to $docker_pid..."

  ip netns exec $docker_pid-cont ip link del vethci$docker_pid
  ip netns exec $docker_pid-bridge ip link del vethce$docker_pid
  ip netns exec $docker_pid-bridge ip link del vethrbe$docker_pid
  ip netns exec $docker_pid-route ip link del vethrbi$docker_pid
  ip netns exec $docker_pid-route ip link del vethrai$docker_pid
  ip link del vethrae$docker_pid
  ip netns del $docker_pid-cont
  ip netns del $docker_pid-route
  ip netns del $docker_pid-bridge

fi
