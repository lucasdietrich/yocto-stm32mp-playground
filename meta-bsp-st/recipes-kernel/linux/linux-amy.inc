SUMMARY = "Kernel for STM32MP157"
HOMEPAGE = "https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git"

inherit kernel
require recipes-kernel/linux/linux-yocto.inc

LINUX_VERSION_MAJOR = "${@d.getVar('PV').split('.')[0]}"

PREMIRRORS:prepend = "\
  https://cdn.kernel.org/pub/linux/kernel/v${LINUX_VERSION_MAJOR}.x/ \
  https://git.kernel.org/torvalds/t/ \n \
"

SRC_URI = "\
        https://cdn.kernel.org/pub/linux/kernel/v${LINUX_VERSION_MAJOR}.x/linux-${PV}.tar.gz \
        file://features/veth.scc file://features/veth.cfg \
        file://features/fs.scc file://features/fs.cfg \
        file://features/usb.scc file://features/usb.cfg \
        file://features/dm.scc file://features/dm.cfg \
        file://features/crypto.scc file://features/crypto.cfg \
"

KERNEL_FEATURES += "veth.scc"
KERNEL_FEATURES += "fs.scc"
KERNEL_FEATURES += "usb.scc"
KERNEL_FEATURES += "dm.scc"
KERNEL_FEATURES += "crypto.scc"

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