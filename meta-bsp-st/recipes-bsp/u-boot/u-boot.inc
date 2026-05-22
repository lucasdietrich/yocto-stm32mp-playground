SUMMARY = "U-Boot for STM32MP"
HOMEPAGE = "https://github.com/u-boot/u-boot.git"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_BRANCH ??= "master"
SRC_URI = " \
    git://github.com/u-boot/u-boot.git;protocol=https;branch=${SRC_BRANCH} \
    file://fw_env.config \
    ${@bb.utils.contains('AMY_DEBUG', '1', 'file://debug.cfg', '', d)} \
"

# specific to mp1
SRC_URI:append:mp1 = " \
    file://0001-Disable-SCMI-for-stm32mp157f-dk2.patch \
    file://0002-Change-custom-boot-command.patch \
    file://amy_defconfig \
    "

S = "${WORKDIR}/git"

PROVIDES += "virtual/u-boot"
PACKAGE_ARCH = "${MACHINE_ARCH}"

EXTRA_OEMAKE = 'CROSS_COMPILE=${TARGET_PREFIX} CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${DEBUG_PREFIX_MAP}"'
EXTRA_OEMAKE += 'HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}"'
EXTRA_OEMAKE += 'STAGING_INCDIR=${STAGING_INCDIR_NATIVE} STAGING_LIBDIR=${STAGING_LIBDIR_NATIVE}'
EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

DEPENDS = "bison-native swig-native"
DEPENDS:append:armv7a = " gcc-arm-none-eabi-native"
DEPENDS:append:aarch64 = " gcc-aarch64-none-elf-native"

UBOOT_CONFIG ?= "amy_defconfig"
UBOOT_DEVICETREE ?= "stm32mp157f-dk2"

do_configure:prepend:mp1() {
    cp ${WORKDIR}/amy_defconfig ${S}/configs/amy_defconfig
}

do_configure:prepend() {
    # Use merge_config.sh to automatically merge config fragments if debug.cfg exists
    if [ -f ${WORKDIR}/debug.cfg ]; then
        ${S}/scripts/kconfig/merge_config.sh -m -O ${B} ${S}/configs/${UBOOT_CONFIG} ${WORKDIR}/debug.cfg
        oe_runmake olddefconfig
    fi

    # Build config
    oe_runmake DEVICE_TREE=${UBOOT_DEVICETREE} ${UBOOT_CONFIG}
}

do_compile:prepend() {
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    # Build u-boot binaries + uboot-initial-env
    oe_runmake -C ${S} O=${B} all
}

do_install() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
    install -m 0644 ${B}/u-boot-initial-env ${D}${sysconfdir}/u-boot-initial-env
}

# u-boot env
PACKAGE_BEFORE_PN += "${PN}-env"
RPROVIDES:${PN}-env += "u-boot-default-env"
FILES:${PN}-env = " \
    ${sysconfdir}/u-boot-initial-env \
    ${sysconfdir}/fw_env.config \
"

RDEPENDS:${PN} += "${PN}-env"

export_binaries() {
    local dest="${1}"

    install -d ${dest}
    install -m 0644 ${S}/u-boot ${dest}/
    install -m 0644 ${S}/u-boot.bin ${dest}/
    install -m 0644 ${S}/u-boot-dtb.bin ${dest}/
    install -m 0644 ${S}/u-boot-nodtb.bin ${dest}/
    install -m 0644 ${S}/u-boot.cfg ${dest}/
    install -m 0644 ${S}/u-boot.dtb ${dest}/
    install -m 0644 ${S}/u-boot.map ${dest}/
}

u_boot_sysroot_populate() {
  export_binaries ${SYSROOT_DESTDIR}${FIP_DIR_UBOOT_BASE}
}
SYSROOT_PREPROCESS_FUNCS =+ "u_boot_sysroot_populate"
SYSROOT_DIRS:append = " ${FIP_DIR_UBOOT_BASE}"

inherit deploy
do_deploy() {
    export_binaries ${DEPLOYDIR}/u-boot
}
addtask do_deploy after do_install

