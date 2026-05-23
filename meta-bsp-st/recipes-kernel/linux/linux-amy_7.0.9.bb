require linux-amy.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/7.0:"

PV = "7.0.9"

SRC_URI[sha256sum] = "aae7690f381f90a58a8a00d0a21b9b4af4ecc9da67a9ec94d468c723c3faf2e7"

KBUILD_DEFCONFIG:mp2 = "defconfig"

SRC_URI += "file://fragment-01-cleanup.cfg"
SRC_URI += "file://fragment-02-addons.cfg"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"