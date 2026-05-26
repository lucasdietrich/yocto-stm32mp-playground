SUMMARY = "Op-TEE"
HOMEPAGE = "https://optee.readthedocs.io/en/latest/index.html"

LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=c1f21c4f72f372ef38a5a4aee55ec173"

SRC_BRANCH ??= "master"
SRC_URI = "git://github.com/OP-TEE/optee_os.git;protocol=https;branch=${SRC_BRANCH}"

S = "${WORKDIR}/git"

PROVIDES += "virtual/optee-os"
PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "dtc-native python3-cryptography-native python3-pyelftools-native"
DEPENDS:append:armv7ve  = " gcc-arm-none-eabi-native"
DEPENDS:append:aarch64 = " gcc-aarch64-none-elf-native"

PACKAGECONFIG ??= ""
PACKAGECONFIG:append:mp2 = " scpfw"
PACKAGECONFIG[scpfw] = "\
    CFG_SCMI_SCPFW=y CFG_SCP_FIRMWARE=${RECIPE_SYSROOT}${SCP_FIRMWARE_SYSROOT_DIR},,\
    scp-firmware-src cmake-native ninja-native\
    "

# OPTEE_PLATFORM is built as ${PLATFORM}-${PLATFORM_FLAVOR}
OPTEE_PLATFORM   ??= "stm32mp1-157C_DK2"
OPTEE_DEVICETREE ??= "stm32mp157c-dk2"

EXTRA_OEMAKE = ""

EXTRA_OEMAKE:append:armv7ve  = " CROSS_COMPILE=arm-none-eabi-"
EXTRA_OEMAKE:append:aarch64 = " CROSS_COMPILE64=aarch64-none-elf-"

EXTRA_OEMAKE += "PLATFORM=${OPTEE_PLATFORM}"
EXTRA_OEMAKE += "CFG_EMBED_DTB_SOURCE_FILE=${OPTEE_DEVICETREE}.dts"

EXTRA_OEMAKE += "CFG_TEE_CORE_NB_CORE=2"

EXTRA_OEMAKE += "CFG_STM32_DEBUG_ACCESS_PTA=y"

EXTRA_OEMAKE:append:mp1 = " CFG_WITH_PAGER=y "
EXTRA_OEMAKE:append:mp1 = " CFG_CORE_RESERVED_SHM=y "
# u-boot scmi driver requires CFG_CORE_DYN_SHM=y
EXTRA_OEMAKE:append:mp1 = " CFG_CORE_DYN_SHM=y "
EXTRA_OEMAKE:append:mp1 = " CFG_STM32MP_REMOTEPROC=y "


# u-boot requires BSEC PTA to be present
# EXTRA_OEMAKE += "CFG_STM32_BSEC_PTA=y"
# EXTRA_OEMAKE:append:mp1 = " CFG_STM32_BSEC=y "
# EXTRA_OEMAKE:append:mp2 = " CFG_STM32_BSEC3=y "

# TODO fix for mp2 (missing RPROC_SIGN_KEY=keys/default.pem)
# EXTRA_OEMAKE += "CFG_REMOTEPROC_PTA=y"

OPTEE_DEBUG ??= "${AMY_DEBUG}"

# CFG_TEE_CORE_LOG_LEVEL might be increase to 3/4 for more verbose output
EXTRA_OEMAKE += "${@bb.utils.contains('OPTEE_DEBUG', '1', '\
    DEBUG=y \
    CFG_TEE_CORE_DEBUG=y \
    CFG_TEE_CORE_LOG_LEVEL=3\
', '', d)}"

EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

do_compile:prepend() {
    # Fixes issue: unrecognized option '-Wl,-O1'
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS
}

export_binaries() {
    local dest="${1}"
    plat="${@d.getVar('OPTEE_PLATFORM').split('-')[0]}"

    install -d ${dest}
    install -m 0644 ${S}/out/arm-plat-${plat}/core/tee-header_v2.bin ${dest}/
    install -m 0644 ${S}/out/arm-plat-${plat}/core/tee-pager_v2.bin ${dest}/
    install -m 0644 ${S}/out/arm-plat-${plat}/core/tee-pageable_v2.bin ${dest}/
    install -m 0644 ${S}/out/arm-plat-${plat}/conf.mk ${dest}/

    # TODO copy all TAs
    mkdir -p ${dest}/ta
}

optee_sysroot_populate() {
  export_binaries ${SYSROOT_DESTDIR}${FIP_DIR_OPTEE_BASE}
}
SYSROOT_PREPROCESS_FUNCS =+ "optee_sysroot_populate"
SYSROOT_DIRS:append = " ${FIP_DIR_OPTEE_BASE}"

inherit deploy
do_deploy() {
    export_binaries ${DEPLOYDIR}/optee-os
}
addtask do_deploy after do_install