SUMMARY = "U-Boot for STM32MP"
HOMEPAGE = "https://github.com/u-boot/u-boot.git"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

inherit pkgconfig

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# if non-empty, this variable should point to a file in the recipe's files directory containing the u-boot defconfig to use
UBOOT_CONFIG_SRC_URI ??= ""
UBOOT_CONFIG ??= "stm32mp15_defconfig"
UBOOT_DEVICETREE ??= "st/stm32mp157f-dk2"

SRC_BRANCH ??= "master"
SRC_URI = " \
    git://github.com/u-boot/u-boot.git;protocol=https;branch=${SRC_BRANCH} \
    file://fw_env.config \
    file://amy.env \
    file://amy-env.cfg \
    ${@oe.utils.conditional('UBOOT_CONFIG_SRC_URI', '', '', 'file://${UBOOT_CONFIG_SRC_URI}', d)} \
"

# mp2 does not support CONFIG_DEBUG_UART
SRC_URI:append:mp1 = " ${@bb.utils.contains('AMY_DEBUG', '1', 'file://debug.cfg', '', d)}"

B = "${WORKDIR}/build"

PROVIDES += "virtual/u-boot"
PACKAGE_ARCH = "${MACHINE_ARCH}"

EXTRA_OEMAKE = 'CROSS_COMPILE=${TARGET_PREFIX} CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${DEBUG_PREFIX_MAP}"'
EXTRA_OEMAKE += 'HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}"'
EXTRA_OEMAKE += 'STAGING_INCDIR=${STAGING_INCDIR_NATIVE} STAGING_LIBDIR=${STAGING_LIBDIR_NATIVE}'
EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

DEPENDS = "bison-native swig-native bc-native dtc-native gnutls-native python3-pyelftools-native"
DEPENDS:append:armv7ve = " gcc-arm-none-eabi-native"
DEPENDS:append:aarch64 = " gcc-aarch64-none-elf-native"

do_configure() {
    # copy the same .env for both supported platforms
    cp ${UNPACKDIR}/amy.env ${S}/board/st/stm32mp1/amy.env
    cp ${UNPACKDIR}/amy.env ${S}/board/st/stm32mp2/amy.env

    # configuration
    base_config="${S}/configs/${UBOOT_CONFIG}"
    configs_to_merge="${base_config}"

    if [ -n "${UBOOT_CONFIG_SRC_URI}" ]; then
        cp ${UNPACKDIR}/${UBOOT_CONFIG_SRC_URI} ${base_config}
    fi

    # Avoid stale state from previous configure runs.
    rm -f ${B}/.config

    # Merge fragments in precedence order: base -> debug (optional) -> amy-env
    if [ -f ${UNPACKDIR}/debug.cfg ]; then
        configs_to_merge="${configs_to_merge} ${UNPACKDIR}/debug.cfg"
    fi

    configs_to_merge="${configs_to_merge} ${UNPACKDIR}/amy-env.cfg"

    ${S}/scripts/kconfig/merge_config.sh -m -O ${B} ${configs_to_merge}

    # Finalize the merged .config in the out-of-tree build directory.
    oe_runmake -C ${S} O=${B} DEVICE_TREE=${UBOOT_DEVICETREE} olddefconfig
}

do_compile() {
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    # Build u-boot binaries
    oe_runmake -C ${S} O=${B} DEVICE_TREE=${UBOOT_DEVICETREE} all

    # Build u-boot-initial-env
    oe_runmake -C ${S} O=${B} DEVICE_TREE=${UBOOT_DEVICETREE} u-boot-initial-env
}

do_install() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${UNPACKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
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
    install -m 0644 ${B}/.config ${dest}/
    install -m 0644 ${B}/u-boot ${dest}/
    install -m 0644 ${B}/u-boot.bin ${dest}/
    install -m 0644 ${B}/u-boot-dtb.bin ${dest}/
    install -m 0644 ${B}/u-boot-nodtb.bin ${dest}/
    install -m 0644 ${B}/u-boot.cfg ${dest}/
    install -m 0644 ${B}/u-boot.dtb ${dest}/
    install -m 0644 ${B}/u-boot.map ${dest}/
    install -m 0644 ${B}/u-boot-initial-env ${dest}/
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

