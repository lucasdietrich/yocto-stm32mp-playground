#!/bin/sh

# set -e

start() {
    flags=""

    # currently factory reset feature depends on u-boot
    # check if fw_printenv command is available
    # then read factory_reset flag in u-boot environment and clear it
    if command -v fw_printenv >/dev/null 2>&1; then
        factory_reset=$(fw_printenv factory_reset | cut -d= -f2)
        fw_setenv factory_reset 0

        # add -f flag to flags if factory_reset was set
        if [ "$factory_reset" = "1" ]; then
            flags="$flags -f"
        fi
    fi

    # create userfs partition (btrfs)
    /usr/bin/userfs -v $flags > /run/userfs.log 2>&1

    # TODO FIXME
    # remount /var/volatile with correct options
    mount -o remount /var/volatile

    # swapfile
    if command -v swapfile > /dev/null 2>&1; then
        swapfile /mnt/userfs/swapfile 256
        swapon /mnt/userfs/swapfile
    fi

    # zswa
    if [ -b /dev/zram0 ]; then
        echo 1 > /sys/block/zram0/reset
        echo 256M > /sys/block/zram0/disksize
        echo 128M > /sys/block/zram0/mem_limit
        mkswap /dev/zram0
        swapon /dev/zram0
    fi
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
        exit 1
        ;;
esac

exit 0