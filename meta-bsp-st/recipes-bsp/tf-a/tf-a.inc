SUMMARY = "Trusted Firmware A"
require tf-a-common.inc

LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://docs/license.rst;md5=b2c740efedc159745b9b31f88ff03dde"

PROVIDES += "virtual/tf-a"
PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "dtc-native"
DEPENDS:append:armv7a = " gcc-arm-none-eabi-native"
DEPENDS:append:aarch64 = " gcc-aarch64-none-elf-native"

TFA_PLATFORM:mp1  = "stm32mp1"
TFA_ARM_MAJOR:mp1 = "7"
TFA_ARM_ARCH:mp1  = "aarch32"

TFA_PLATFORM:mp2  = "stm32mp2"
TFA_ARM_MAJOR:mp2 = "8"
TFA_ARM_ARCH:mp2  = "aarch64"

TFA_DEVICETREE ??= "stm32mp157c-dk2"

PACKAGECONFIG ??= "${@bb.utils.contains('AMY_DEBUG', '1', 'debug', '', d)}"
PACKAGECONFIG[debug] = "DEBUG=1 LOG_LEVEL=40,"

EXTRA_OEMAKE = ""

EXTRA_OEMAKE:append:armv7a = " CROSS_COMPILE=arm-none-eabi- "
EXTRA_OEMAKE:append:aarch64 = " CROSS_COMPILE=aarch64-none-elf- "

EXTRA_OEMAKE += "PLAT=${TFA_PLATFORM}"
EXTRA_OEMAKE += "ARCH=${TFA_ARM_ARCH}"
EXTRA_OEMAKE += "ARM_ARCH_MAJOR=${TFA_ARM_MAJOR}"
EXTRA_OEMAKE += "STM32MP_SDMMC=1"
EXTRA_OEMAKE += "DTB_FILE_NAME=${TFA_DEVICETREE}.dtb"
EXTRA_OEMAKE:append:mp1 = " STM32MP1_OPTEE_IN_SYSRAM=1"
EXTRA_OEMAKE:append:mp2 = " STM32MP_LPDDR4_TYPE=1"

EXTRA_OEMAKE += "${PACKAGECONFIG_CONFARGS}"

do_compile:prepend() {
    # Fixes issue: unrecognized option '-Wl,-O1'
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    # Fixes issue: aarch64-poky-linux-as: unrecognized option '-x'
    # Allow TF-A to derive AS from CROSS_COMPILE instead of using the Linux-sysroot assembler
    unset AS
}

export_binaries() {
    local dest="${1}"
    dirname="${@bb.utils.contains('PACKAGECONFIG', 'debug', 'debug', 'release', d)}"

    install -d ${dest}
    install -m 0644 ${S}/build/${TFA_PLATFORM}/${dirname}/tf-a-${TFA_DEVICETREE}.stm32 ${dest}/
    
    install -d ${dest}/bl31
    if [ -f ${S}/build/${TFA_PLATFORM}/${dirname}/bl31.bin ]; then
        install -m 0644 ${S}/build/${TFA_PLATFORM}/${dirname}/bl31.bin ${dest}/bl31/
    fi

    install -d ${dest}/fdts
    install -m 0644 ${S}/build/${TFA_PLATFORM}/${dirname}/fdts/${TFA_DEVICETREE}-bl2.dtb ${dest}/fdts/
    install -m 0644 ${S}/build/${TFA_PLATFORM}/${dirname}/fdts/${TFA_DEVICETREE}-bl31.dtb ${dest}/fdts/
    install -m 0644 ${S}/build/${TFA_PLATFORM}/${dirname}/fdts/${TFA_DEVICETREE}-fw-config.dtb ${dest}/fdts/
}

tf_a_sysroot_populate() {
  export_binaries ${SYSROOT_DESTDIR}${FIP_DIR_TFA_BASE}
}
SYSROOT_PREPROCESS_FUNCS =+ "tf_a_sysroot_populate"
SYSROOT_DIRS:append = " ${FIP_DIR_TFA_BASE}"

inherit deploy
do_deploy() {
    export_binaries ${DEPLOYDIR}/tf-a
}
addtask do_deploy after do_install