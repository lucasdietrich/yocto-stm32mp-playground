require linux-amy.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/7.0:"

SRC_URI += " \
        file://0001-Add-custom-amy-devicetree.patch \
        file://0002-feat-Add-USB-OTG-HS-support.patch \
"

SRC_URI:append:mp1 = " file://amy-stm32mp1_defconfig "

# TODO this needs to be reviewed and made common with the mp1 configuration
SRC_URI:append:mp2 = " file://amy-stm32mp2_defconfig "

do_configure:prepend:mp1() {
    cp ${WORKDIR}/amy-stm32mp1_defconfig defconfig
}

do_configure:prepend:mp2() {
    cp ${WORKDIR}/amy-stm32mp2_defconfig defconfig
}

PV = "7.0.10"
SRC_URI[sha256sum] = "573690074720e5703db81074ac4c0102d8e135252af59ee4511c59b20c3c2a46"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"