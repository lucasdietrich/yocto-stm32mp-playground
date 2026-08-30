#!/bin/sh

set -e

STAMP_FILE=/containers/.done
if [ -f $STAMP_FILE ]; then
    echo "Container runtime already initialized."
    exit 0
fi

cd /containers

mkdir -p oci-bundle-static oci-bundle
mkdir -p /tmp/upper /tmp/work
mount -o loop example-container-image.img oci-bundle-static
mount -t overlay overlay -o lowerdir=/containers/oci-bundle-static,upperdir=/tmp/upper,workdir=/tmp/work /containers/oci-bundle

touch $STAMP_FILE