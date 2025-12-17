#!/usr/bin/bash

dev=/dev/sdd
machine=${1-"stm32mp157f-dk2"}
image=${2:-"amy"}

if [ ! -b $dev ]; then
    echo "No such device: $dev"
    exit 1
fi

img=images/$machine/$image-image-$machine-sdcard.img

sudo dd if=$img of=$dev bs=4M status=progress