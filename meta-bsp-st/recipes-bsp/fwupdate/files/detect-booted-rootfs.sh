#!/bin/sh

ROOTFS_A_UUID="6fd65744-f376-48ad-bffc-cda83df4d37e"
ROOTFS_B_UUID="836a91dd-66c9-40a2-840c-c35428b09a91"

BOOTED=$(findmnt -n -o PARTUUID /)

case "$BOOTED" in
    "$ROOTFS_A_UUID")
        CURRENT_ROOTFS_UUID="$ROOTFS_A_UUID"
        OTHER_ROOTFS_UUID="$ROOTFS_B_UUID"
        CURRENT_ROOTFS_SLOT="A"
        OTHER_ROOTFS_SLOT="B"
        ;;
    "$ROOTFS_B_UUID")
        CURRENT_ROOTFS_UUID="$ROOTFS_B_UUID"
        OTHER_ROOTFS_UUID="$ROOTFS_A_UUID"
        CURRENT_ROOTFS_SLOT="B"
        OTHER_ROOTFS_SLOT="A"
        ;;
    *)
        echo "Could not determine boot slot from cmdline" >&2
        return 1
        ;;
esac