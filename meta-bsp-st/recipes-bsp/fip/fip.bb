SUMMARY = "FIP package for STM32MP2"
LICENSE = "CLOSED"

COMPATIBLE_MACHINE = "(mp2)"
PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS += "tf-a-tools-native"
DEPENDS += "virtual/optee-os"
DEPENDS += "virtual/tf-a"
DEPENDS += "stm32mp-ddr-phy"
DEPENDS += "virtual/u-boot"

do_compile() {
    fiptool create \
        --fw-config ${RECIPE_SYSROOT}${FIP_DIR_TFA_BASE}/fdts/stm32mp257f-dk-fw-config.dtb \
        --soc-fw ${RECIPE_SYSROOT}${FIP_DIR_TFA_BASE}/bl31/bl31.bin \
        --soc-fw-config ${RECIPE_SYSROOT}${FIP_DIR_TFA_BASE}/fdts/stm32mp257f-dk-bl31.dtb \
        --ddr-fw ${RECIPE_SYSROOT}${FIP_DIR_DDR_PHY_BASE}/lpddr4_pmu_train.bin \
        --tos-fw ${RECIPE_SYSROOT}${FIP_DIR_OPTEE_BASE}/tee-header_v2.bin \
        --tos-fw-extra1 ${RECIPE_SYSROOT}${FIP_DIR_OPTEE_BASE}/tee-pager_v2.bin \
        --tos-fw-extra2 ${RECIPE_SYSROOT}${FIP_DIR_OPTEE_BASE}/tee-pageable_v2.bin \
        --nt-fw ${RECIPE_SYSROOT}${FIP_DIR_UBOOT_BASE}/u-boot-nodtb.bin \ 
        --hw-config ${RECIPE_SYSROOT}${FIP_DIR_UBOOT_BASE}/u-boot.dtb   \ 
        fip-${MACHINE}.bin

    fiptool info fip-${MACHINE}.bin > fip-${MACHINE}.info.txt
}

inherit deploy
do_deploy() {
    install -d ${DEPLOYDIR}/fip
    install -m 0644 fip-${MACHINE}.bin ${DEPLOYDIR}/fip/
    install -m 0644 fip-${MACHINE}.info.txt ${DEPLOYDIR}/fip/
}
addtask deploy before do_build after do_compile