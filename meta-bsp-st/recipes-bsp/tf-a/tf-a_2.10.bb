SUMMARIZE = "Trusted Firmware A"
HOMEPAGE = "https://www.trustedfirmware.org/projects/tf-a/"

LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://docs/license.rst;md5=b2c740efedc159745b9b31f88ff03dde"

SRC_URI = "git://github.com/TrustedFirmware-A/trusted-firmware-a.git;protocol=https;branch=lts-v2.10"
SRCREV = "bf6bc80a5f2d4a24ac5950314b3e79d61a7e6cee"

S = "${WORKDIR}/git"

PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "gcc-arm-none-eabi-native dtc-native"

TFA_DEVICETREE = "stm32mp157c-dk2"

#
# Instructions to build the TF-A for STM32MP1
#
# . /opt/amy/1.0/environment-setup-cortexa7t2hf-neon-vfpv4-poky-linux-gnueabi && \
#     unset LDFLAGS && unset CFLAGS && unset CPPFLAGS && \
#     make CROSS_COMPILE=arm-none-eabi- \
#     STM32MP_SDMMC=1 \
#     PLAT=stm32mp1 \ARCH=aarch32 ARM_ARCH_MAJOR=7 \
#     DTB_FILE_NAME=stm32mp157c-dk2.dtb \
#     LOG_LEVEL=40 \
#     -j16
EXTRA_OEMAKE = "CROSS_COMPILE=arm-none-eabi-"

# Fixes issue: unrecognized option '-Wl,-O1'
EXTRA_OEMAKE += "LDFLAGS="

EXTRA_OEMAKE += "STM32MP_SDMMC=1"
EXTRA_OEMAKE += "PLAT=stm32mp1"
EXTRA_OEMAKE += "ARCH=aarch32"
EXTRA_OEMAKE += "ARM_ARCH_MAJOR=7"
EXTRA_OEMAKE += "DTB_FILE_NAME=${TFA_DEVICETREE}.dtb"

EXTRA_OEMAKE += "DEBUG=0"
EXTRA_OEMAKE += "LOG_LEVEL=40"

inherit deploy

do_deploy() {
    mkdir -p ${DEPLOYDIR}/tf-a
    cp ${S}/build/stm32mp1/release/tf-a-${TFA_DEVICETREE}.stm32 ${DEPLOYDIR}/tf-a/
}

addtask do_deploy after do_install