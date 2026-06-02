#!/bin/bash

fiptool=build-mp2/tmp/sysroots-components/x86_64/tf-a-tools-native/usr/bin/fiptool

machine="mp2"

deploy_dir=build-mp2/tmp/deploy/images/mp2
optee="$deploy_dir/optee-os"

# uncomment to use custom optee binaries instead of the ones from the deploy directory
# optee="/home/lucas/yocto/mp2/optee_os/out/arm-plat-stm32mp2/core"

rm -f tmp/fip-$machine.bin

$fiptool create \
    --fw-config $deploy_dir/tf-a/fdts/stm32mp257f-dk-fw-config.dtb \
    --soc-fw $deploy_dir/tf-a/bl31/bl31.bin \
    --soc-fw-config $deploy_dir/tf-a/fdts/stm32mp257f-dk-bl31.dtb \
    --ddr-fw $deploy_dir/stm32mp-ddr-phy/lpddr4_pmu_train.bin \
    --tos-fw $optee/tee-header_v2.bin \
    --tos-fw-extra1 $optee/tee-pager_v2.bin \
    --tos-fw-extra2 $optee/tee-pageable_v2.bin \
    --nt-fw $deploy_dir/u-boot/u-boot-nodtb.bin \
    --hw-config $deploy_dir/u-boot/u-boot.dtb \
    tmp/fip-$machine.bin

deva="/dev/disk/by-partlabel/fip-a"
devb="/dev/disk/by-partlabel/fip-b"

if [ -b $deva ]; then
    sudo dd if=tmp/fip-$machine.bin of=$deva bs=4M status=progress
    sudo dd if=tmp/fip-$machine.bin of=$devb bs=4M count=1 conv=fsync
else
    echo "No partition with label 'fip-a' found. Please flash tmp/fip-$machine.bin to the appropriate partition."
fi