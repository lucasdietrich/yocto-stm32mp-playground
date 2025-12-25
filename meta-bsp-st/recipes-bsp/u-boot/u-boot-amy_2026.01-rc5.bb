SUMMARIZE = "U-Boot for STM32MP1"
HOMEPAGE = "https://github.com/u-boot/u-boot.git"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

S = "${WORKDIR}/git"

PROVIDES += "virtual/u-boot"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "git://github.com/u-boot/u-boot.git;protocol=https;branch=master"
SRCREV = "0f6ff53d55ba254de8a995c2a2f5a313acd40ac7"

SRC_URI += "file://0001-Disable-SCMI-for-stm32mp157f-dk2.patch \
            file://0002-Change-custom-boot-command.patch \
            file://amy_defconfig \
            ${@bb.utils.contains('AMY_DEBUG', '1', 'file://debug.cfg', '', d)} \
            "

DEPENDS = "gcc-arm-none-eabi-native bison-native swig-native"

EXTRA_OEMAKE = 'CROSS_COMPILE=arm-none-eabi- CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${DEBUG_PREFIX_MAP}"'
EXTRA_OEMAKE += 'HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}"'
EXTRA_OEMAKE += 'STAGING_INCDIR=${STAGING_INCDIR_NATIVE} STAGING_LIBDIR=${STAGING_LIBDIR_NATIVE}'

EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

do_configure:prepend() {
    cp ${WORKDIR}/amy_defconfig ${S}/configs/amy_defconfig
    
    # Use merge_config.sh to automatically merge config fragments if debug.cfg exists
    if [ -f ${WORKDIR}/debug.cfg ]; then
        ${S}/scripts/kconfig/merge_config.sh -m -O ${B} ${S}/configs/amy_defconfig ${WORKDIR}/debug.cfg
        oe_runmake olddefconfig
    else
        oe_runmake amy_defconfig
    fi
}

do_compile:prepend() {
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS
}

inherit deploy

do_deploy() {
    mkdir -p ${DEPLOYDIR}/u-boot
    cp ${S}/u-boot.bin ${DEPLOYDIR}/u-boot
}

addtask do_deploy after do_install