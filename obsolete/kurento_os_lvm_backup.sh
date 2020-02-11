#!/bin/bash -x
#
# Openstack LVM Volume Backup
#
# Usage:
# ./kurento-os-lvm-backup VOL_ID VOL_NAME
#		VOL_ID: Volume identifiaction as OpenStack name it
#		VOL_NAME: Descriptive name for the volume and tarball
#
# Based: https://docs.openstack.org/cinder/latest/admin/blockstorage-backup-disks.html

set -eu -o pipefail

export PATH=$PATH:/sbin

DATE=$(date +'%Y-%m-%d')
VOL_PREFIX="/dev/cinder-volumes"

[ -z $1 ] && exit 1
VOL_ID=$1
[ -z $2 ] && exit 1
VOL_NAME=$2

SIZE=$(lvdisplay $VOL_PREFIX/volume-$VOL_ID | grep "LV Size" | awk '{ print $3 }')
BACKUPDIR="/backups"

# Creating the snapshot
lvcreate --size ${SIZE}G --snapshot --name ${VOL_NAME}-snap ${VOL_PREFIX}/volume-${VOL_ID}

# Creating file table
kpartx -a ${VOL_PREFIX}/${VOL_NAME}-snap

# Getting device name and mounting partition
sleep 5
VOL_FILETABLE_NAME=$(ls -1 /dev/mapper/ | grep "snap1$")
mount /dev/mapper/${VOL_FILETABLE_NAME} /mnt

# Creating tarball
tar czf ${BACKUPDIR}/${VOL_NAME}-${DATE}.tar.gz /mnt

# Cleaning the house

# Umount partition
umount /mnt

# Deleting file table
kpartx -d ${VOL_PREFIX}/${VOL_NAME}-snap

# Removing snapshot
lvremove -f ${VOL_PREFIX}/${VOL_NAME}-snap
