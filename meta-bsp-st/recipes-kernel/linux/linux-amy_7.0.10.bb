require linux-amy.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/7.0:"

SRC_URI += " \
        file://defconfig \
        file://0001-Add-custom-amy-devicetree.patch \
"

PV = "7.0.10"
SRC_URI[sha256sum] = "573690074720e5703db81074ac4c0102d8e135252af59ee4511c59b20c3c2a46"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"