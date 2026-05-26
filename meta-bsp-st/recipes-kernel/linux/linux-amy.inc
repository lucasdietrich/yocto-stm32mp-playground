SUMMARY = "Kernel for STM32MP157"

inherit kernel
require recipes-kernel/linux/linux-yocto.inc

LINUX_VERSION_MAJOR = "${@d.getVar('PV').split('.')[0]}"

PREMIRRORS:prepend = "\
  https://cdn.kernel.org/pub/linux/kernel/v${LINUX_VERSION_MAJOR}.x/ \
  https://git.kernel.org/torvalds/t/ \n \
"

SRC_URI = "\
        https://cdn.kernel.org/pub/linux/kernel/v${LINUX_VERSION_MAJOR}.x/linux-${PV}.tar.gz \
        file://features/veth.scc \
        "

KERNEL_FEATURES += "veth.scc"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

LINUX_VERSION ?= "${PV}"
LINUX_VERSION_EXTENSION:append = "-amy"

KERNEL_DEPLOYSUBDIR = "kernel"

S="${WORKDIR}/linux-${LINUX_VERSION}"

COMPATIBLE_MACHINE = "^(mp1|mp2)$"
KCONFIG_MODE = "alldefconfig"

DEPENDS += " lz4-native "

do_deploy:append() {
    install -m 0644 ${B}/.config ${DEPLOYDIR}/${KERNEL_DEPLOYSUBDIR}/.config
}