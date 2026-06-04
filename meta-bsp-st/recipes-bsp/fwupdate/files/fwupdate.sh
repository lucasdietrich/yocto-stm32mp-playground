#!/bin/sh

set -e

usage() {
    echo "Usage: $0 [apply_new_update | abort_update]"
    exit 1
}

ustate=$(fw_printenv ustate | cut -d= -f2)

. /usr/lib/fwupdate/detect-booted-rootfs.sh

echo "$0 ustate: $ustate current rootfs UUID: $CURRENT_ROOTFS_UUID"


# Handle arguments
case "$1" in
    apply_new_update)
        # swupdate has set ustate to 7 (STATE_IN_PROGRESS)

        echo "Applying new update..."

        # select the other root partition
        fw_setenv rootfs_partuuid "$OTHER_ROOTFS_UUID"

        ;;
    abort_update)
        echo "Aborting update..."

        # set ustate to 0 to indicate no update is no longer in progress
        fw_setenv ustate 0

        # revert to the current root partition
        fw_setenv rootfs_partuuid "$CURRENT_ROOTFS_UUID"

        ;;
    "" | -h | --help)
        usage
        ;;
    *)
        echo "Error: Unknown argument '$1'"
        usage
        ;;
esac