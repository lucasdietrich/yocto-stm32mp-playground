SUMMARY = "Kernel for STM32MP157"

inherit kernel
require recipes-kernel/linux/linux-yocto.inc

SRC_URI = "\
        https://cdn.kernel.org/pub/linux/kernel/v6.x/linux-${PV}.tar.xz \
        file://defconfig \
        "

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

LINUX_VERSION ?= "${PV}"
LINUX_VERSION_EXTENSION:append = "-amy"

KERNEL_DEPLOYSUBDIR = "kernel"

S="${WORKDIR}/linux-${LINUX_VERSION}"

COMPATIBLE_MACHINE = "dk2"
KCONFIG_MODE = "alldefconfig"

DEPENDS += " lz4-native "