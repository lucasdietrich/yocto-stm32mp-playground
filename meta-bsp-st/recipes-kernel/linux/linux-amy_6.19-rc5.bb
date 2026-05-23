require linux-amy.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/6.19:"

SRC_URI += " \
        file://defconfig \
        file://0001-Add-custom-amy-devicetree.patch \
"

PV = "6.19-rc5"
SRC_URI[sha256sum] = "f8ad1215e6e43e677fafc5d0416daa3a02271866300368b7a3917d14ecefa9cd"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"