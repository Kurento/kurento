#!/bin/bash
set -eu

# This script is aimed to help with the power off and on of the OS cluster
# From time to time, we've to shut down the instances because of power cut
# This script will shut off all the instances which are actually running 
# and keep then in files so when the power come back it knows which ones 
# should be running.

# Change to start to power on intances.
MODE=stop
INSTANCES_SERVICE=${PWD}/instances-service.txt
INSTANCES_TESTING=${PWD}/instances-testing.txt
INSTANCES_KUBERNETES=${PWD}/instances-kubernetes.txt

if [ "${MODE}" == "stop" ]; then

	## Services
	echo "" > ${INSTANCES_SERVICE}
	source /home/nordri/services-openrc
	INSTANCES=$(openstack server list | grep ACTIVE | awk '{ print $4 }')
	for INSTANCE in ${INSTANCES}
	do
		echo powering off ${INSTANCE}...
		openstack server stop ${INSTANCE}
		echo ${INSTANCE} >> ${INSTANCES_SERVICE}
		sleep 10
	done

	## Testing
	echo "" > ${INSTANCES_TESTING}
	source /home/nordri/testing-openrc
	INSTANCES=$(openstack server list | grep ACTIVE | awk '{ print $4 }')
	for INSTANCE in ${INSTANCES}
	do
		echo powering off ${INSTANCE}...
		openstack server stop ${INSTANCE}
		echo $INSTANCE >> ${INSTANCES_TESTING}
		sleep 10
	done

	## Kubernetes
	echo "" > ${INSTANCES_KUBERNETES}
	source /home/nordri/kubernetes-openrc
	INSTANCES=$(openstack server list | grep ACTIVE | awk '{ print $4 }')
	for INSTANCE in ${INSTANCES}
	do
		echo powering off ${INSTANCE}...
		openstack server stop ${INSTANCE}
		echo $INSTANCE >> ${INSTANCES_KUBERNETES}
		sleep 10
	done
else
	## Services
	source /home/nordri/services-openrc
	for INSTANCE in $(cat ${INSTANCES_SERVICE}) 
	do
		echo powering up ${INSTANCE}...
		openstack server start ${INSTANCE}
		sleep 60
	done

	## Testing
	source /home/nordri/testing-openrc
	for INSTANCE in $(cat ${INSTANCES_TESTING}) 
	do
		echo powering up ${INSTANCE}...
		openstack server start ${INSTANCE}
		sleep 60
	done

	## kubernetes
	source /home/nordri/kubernetes-openrc
	for INSTANCE in $(cat ${INSTANCES_KUBERNETES}) 
	do
		echo powering up ${INSTANCE}...
		openstack server start ${INSTANCE}
		sleep 60
	done

	rm ${INSTANCES_SERVICE}
	rm ${INSTANCES_TESTING}
	rm ${INSTANCES_KUBERNETES}
fi
