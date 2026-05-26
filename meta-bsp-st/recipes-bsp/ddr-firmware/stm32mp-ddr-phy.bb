SUMMARY = "Firmware for DDR PHY on STM32MP"
LICENSE = "Proprietary"

HOMEPAGE = "https://github.com/STMicroelectronics/stm32-ddr-phy-binary"

LIC_FILES_CHKSUM = "file://${S}/LICENSE.md;md5=bb8009a40d2aca1844e6eb550bf8a6bc"

SRC_URI = "git://github.com/STMicroelectronics/stm32-ddr-phy-binary.git;protocol=https;branch=main"
SRCREV = "77447cf214eadf128e487fcb10a4a78cd4ab6d56"

PV = "A2022.11"

S = "${WORKDIR}/git"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "(mp2)"

do_compile() {
	:
}

export_binaries() {
    local dest="${1}"

    install -d ${dest}
    install -m 0644 ${S}/stm32mp2/* ${dest}
}

stm32mp_ddr_phy_sysroot_populate() {
  export_binaries ${SYSROOT_DESTDIR}${FIP_DIR_DDR_PHY_BASE}
}
SYSROOT_PREPROCESS_FUNCS =+ "stm32mp_ddr_phy_sysroot_populate"
SYSROOT_DIRS:append = " ${FIP_DIR_DDR_PHY_BASE}"
