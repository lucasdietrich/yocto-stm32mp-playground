SUMMARIZE = "Op-TEE"
HOMEPAGE = "https://optee.readthedocs.io/en/latest/index.html"

LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=c1f21c4f72f372ef38a5a4aee55ec173"

SRC_URI = "git://github.com/OP-TEE/optee_os.git;protocol=https;branch=master"
SRCREV = "86846f4fdf14f25b50fd64a87888ca9fe85a9e2b"

S = "${WORKDIR}/git"

PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "gcc-arm-none-eabi-native dtc-native python3-cryptography-native python3-pyelftools-native"

EXTRA_OEMAKE = "CROSS_COMPILE=arm-none-eabi-"

EXTRA_OEMAKE += "PLATFORM=stm32mp1-157C_DK2"
EXTRA_OEMAKE += "CFG_EMBED_DTB_SOURCE_FILE=stm32mp157c-dk2.dts"

EXTRA_OEMAKE += "CFG_WITH_PAGER=y"
EXTRA_OEMAKE += "CFG_CORE_RESERVED_SHM=y"
# u-boot scmi driver requires CFG_CORE_DYN_SHM=y
EXTRA_OEMAKE += "CFG_CORE_DYN_SHM=y"

EXTRA_OEMAKE += "CFG_STM32MP_REMOTEPROC=y"
EXTRA_OEMAKE += "CFG_TEE_CORE_NB_CORE=2"

# CFG_TEE_CORE_LOG_LEVEL might be increase to 3/4 for more verbose output
EXTRA_OEMAKE += "${@bb.utils.contains('AMY_DEBUG', '1', '\
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

inherit deploy

do_deploy() {
    mkdir -p ${DEPLOYDIR}/optee-os
    cp ${S}/out/arm-plat-stm32mp1/core/tee-header_v2.bin ${DEPLOYDIR}/optee-os
    cp ${S}/out/arm-plat-stm32mp1/core/tee-pager_v2.bin ${DEPLOYDIR}/optee-os
    cp ${S}/out/arm-plat-stm32mp1/core/tee-pageable_v2.bin ${DEPLOYDIR}/optee-os
}

addtask do_deploy after do_install