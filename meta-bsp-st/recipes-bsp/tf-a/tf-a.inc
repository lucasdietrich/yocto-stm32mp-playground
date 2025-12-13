SUMMARIZE = "Trusted Firmware A"
HOMEPAGE = "https://www.trustedfirmware.org/projects/tf-a/"

LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://docs/license.rst;md5=b2c740efedc159745b9b31f88ff03dde"

S = "${WORKDIR}/git"

PROVIDES += "virtual/tf-a"
PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "gcc-arm-none-eabi-native dtc-native"

TFA_DEVICETREE ??= "stm32mp157c-dk2"

PACKAGECONFIG ??= "${@bb.utils.contains('AMY_DEBUG', '1', 'debug', '', d)}"
PACKAGECONFIG[debug] = "DEBUG=1 LOG_LEVEL=40,"

#
# Instructions to build the TF-A for STM32MP1
#
# . /opt/amy/1.0/environment-setup-cortexa7t2hf-neon-vfpv4-poky-linux-gnueabi && \
#     unset LDFLAGS && unset CFLAGS && unset CPPFLAGS && \
#     make CROSS_COMPILE=arm-none-eabi- \
#     STM32MP_SDMMC=1 \
#     PLAT=stm32mp1 \
#     ARCH=aarch32 ARM_ARCH_MAJOR=7 \
#     DTB_FILE_NAME=stm32mp157c-dk2.dtb \
#     LOG_LEVEL=40 \
#     -j16
EXTRA_OEMAKE = "CROSS_COMPILE=arm-none-eabi-"

EXTRA_OEMAKE += "PLAT=stm32mp1"
EXTRA_OEMAKE += "ARCH=aarch32"
EXTRA_OEMAKE += "ARM_ARCH_MAJOR=7"
EXTRA_OEMAKE += "STM32MP_SDMMC=1"
EXTRA_OEMAKE += "DTB_FILE_NAME=${TFA_DEVICETREE}.dtb"
EXTRA_OEMAKE += "STM32MP1_OPTEE_IN_SYSRAM=1"

EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

do_compile:prepend() {
    # Fixes issue: unrecognized option '-Wl,-O1'
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS
}

inherit deploy

do_deploy() {
    mkdir -p ${DEPLOYDIR}/tf-a
    dirname="${@bb.utils.contains('PACKAGECONFIG', 'debug', 'debug', 'release', d)}"
    cp ${S}/build/stm32mp1/${dirname}/tf-a-${TFA_DEVICETREE}.stm32 ${DEPLOYDIR}/tf-a/
}

addtask do_deploy after do_install