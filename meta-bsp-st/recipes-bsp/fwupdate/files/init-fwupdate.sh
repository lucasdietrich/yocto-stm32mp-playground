#!/bin/sh

# set -e

. /usr/lib/fwupdate/detect-booted-rootfs.sh

create_ab_symlinks() {
    # Resolve to actual device nodes
    DEV_A=$(readlink -f /dev/disk/by-partuuid/$ROOTFS_A_UUID)
    DEV_B=$(readlink -f /dev/disk/by-partuuid/$ROOTFS_B_UUID)

    mkdir -p /dev/swu
    
    ln -sf "$DEV_A" /dev/swu/rootfs-a
    ln -sf "$DEV_B" /dev/swu/rootfs-b

    if [ "$BOOTED" = "$ROOTFS_A_UUID" ]; then
        echo "swupdate-slots: current slot is $CURRENT_ROOTFS_SLOT ($DEV_A)"
        ln -sf "$DEV_A" /dev/swu/current-rootfs
        ln -sf "$DEV_B" /dev/swu/other-rootfs
    elif [ "$BOOTED" = "$ROOTFS_B_UUID" ]; then
        echo "swupdate-slots: current slot is $CURRENT_ROOTFS_SLOT ($DEV_B)"
        ln -sf "$DEV_B" /dev/swu/current-rootfs
        ln -sf "$DEV_A" /dev/swu/other-rootfs
    else
        echo "swupdate-slots: could not determine boot slot from cmdline" >&2
        exit 1
    fi
}

handle_pending_update() {
    # https://sbabic.github.io/swupdate/swupdate-best-practise.html
    ustate=$(fw_printenv ustate | cut -d= -f2)

    case "$ustate" in
        0)
            echo "fwupdate: No update pending."
            ;;
        1)
            echo "fwupdate: A new update was installed (under test)."

            # swupdate has finished installed th update, the ustate is set to 1 (STATE_INSTALLED)
            # we should normally test the update here, but for now we just assume it is ok
            # and we just clear the ustate flag (STATE_OK)
            fw_setenv ustate 0
            fw_setenv bootcount 0
            ;;
        3)
            echo "fwupdate: Update failed, rolling back to previous rootfs. (TODO)"
            ;;
        *)
            echo "fwupdate: Unknown ustate: $ustate"
            ;;
    esac
}

start() {
    handle_pending_update
    create_ab_symlinks
}

stop() {
    :
}

case "$1" in
    start)
       start
       ;;
    stop)
       stop
       ;;
    status)
       ;;
    *)
       echo "Usage: $0 {start|stop}"
esac

exit 0