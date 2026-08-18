require linux-amy.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/7.1:"

SRC_URI += " \
        file://0001-Add-custom-amy-devicetree.patch \
        file://0002-feat-Add-USB-OTG-HS-support.patch \
"

SRC_URI:append:mp1 = " file://amy-stm32mp1_defconfig "

do_configure:prepend:mp1() {
    cp ${UNPACKDIR}/amy-stm32mp1_defconfig defconfig
}

PV = "7.1.1"
SRC_URI[sha256sum] = "78d177a7e9b64cdb0ee1bd374c80e155ac22e03ca90fa5e358a91a39f39b8602"

# arch/arm/include/generated/asm/mach-types.h embeds the absolute source path
# (via gen-mach-types) in a comment; that path isn't covered by the default
# S/B prefix-map remapping, so it trips the buildpaths QA check on the -src
# package. It's just a generated comment, not functional code, so skip it.
INSANE_SKIP:${PN}-src += "buildpaths"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"