SUMMARY = "STM32MP2 CM33 USBPD firmware - built from STM32CubeMP2 source"
DESCRIPTION = "Compiles the CM33 USBPD_DRP_UCSI firmware from the STM32CubeMP2 git \
repository using gcc-arm-none-eabi-native + cmake-native, installs it to \
/lib/firmware/, and enables a systemd service that starts the CM33 via remoteproc."

LICENSE = "Apache-2.0 & MIT & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://License.md;md5=012a8d78c6f636371ad889eadb15885c"

SRC_URI = " \
    git://github.com/STMicroelectronics/STM32CubeMP2.git;protocol=https;nobranch=1 \
    file://cm33-usbpd-load.sh \
    file://cm33-usbpd-init \
"
# v1.3.1 - same revision used by meta-st-stm32mp
SRCREV = "2f7258aa45e916777ffb4f6e1b5590f65304378d"

PV = "1.3.1"

S = "${WORKDIR}/git"

USBPD_PROJECT_DIR = "${S}/Projects/STM32MP257F-DK/Demonstrations/USBPD_DRP_UCSI"
USBPD_FW_NAME    = "USBPD_DRP_UCSI_CM33_NonSecure_stripped.elf"

DEPENDS = "cmake-native gcc-arm-none-eabi-native"

inherit update-rc.d

INITSCRIPT_NAME = "cm33-usbpd"
INITSCRIPT_PARAMS = "start 70 S . stop 30 0 6 ."

do_configure() {
    cmake \
        -G "Unix Makefiles" \
        --fresh \
        -S ${USBPD_PROJECT_DIR} \
        -B ${USBPD_PROJECT_DIR}/build
}

do_compile() {
    make -C ${USBPD_PROJECT_DIR}/build all
    # Strip debug info to produce the _stripped variant expected by remoteproc
    arm-none-eabi-strip \
        -o ${USBPD_PROJECT_DIR}/build/${USBPD_FW_NAME} \
        ${USBPD_PROJECT_DIR}/build/USBPD_DRP_UCSI_CM33_NonSecure.elf
}

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware
    install -m 0644 \
        ${USBPD_PROJECT_DIR}/build/${USBPD_FW_NAME} \
        ${D}${nonarch_base_libdir}/firmware/

    install -d ${D}${base_sbindir}
    install -m 0755 ${WORKDIR}/cm33-usbpd-load.sh ${D}${base_sbindir}/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/cm33-usbpd-init \
        ${D}${sysconfdir}/init.d/cm33-usbpd
}

FILES:${PN} = " \
    ${nonarch_base_libdir}/firmware/${USBPD_FW_NAME} \
    ${base_sbindir}/cm33-usbpd-load.sh \
    ${sysconfdir}/init.d/cm33-usbpd \
"

COMPATIBLE_MACHINE = "^(mp2)$"

# The firmware is a bare-metal ARM32 (Cortex-M33) ELF inside an AArch64 package — expected.
INSANE_SKIP:${PN} = "arch"
