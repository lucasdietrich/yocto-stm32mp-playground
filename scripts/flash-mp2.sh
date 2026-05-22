#!/usr/bin/bash

dev=/dev/sdd
machine=${1-"mp2"}
image=${2:-"amy-image-minimal"}

if [ ! -b $dev ]; then
    echo "No such device: $dev"
    exit 1
fi

img=build-mp2/tmp/deploy/images/$machine/$image-$machine-sdcard.img

sudo dd if=$img of=$dev bs=4M status=progress