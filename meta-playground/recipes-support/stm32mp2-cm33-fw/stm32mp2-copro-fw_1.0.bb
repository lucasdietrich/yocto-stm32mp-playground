SUMMARY = "STM32MP2 Coprocessor firmwares built from STM32CubeMP2 source"
DESCRIPTION = "Compiles the CM33 (USBPD_DRP_UCSI) and CM0+ (CM0PLUS_DEMO) firmwares for the STM32MP2 coprocessor"

LICENSE = "Apache-2.0 & MIT & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://License.md;md5=012a8d78c6f636371ad889eadb15885c"

SRC_URI = " \
    git://github.com/STMicroelectronics/STM32CubeMP2.git;protocol=https;nobranch=1 \
    file://0001-build-cmake-make-_TRACE-define-opt-in-via-TRACE-opti.patch \
    file://cm33-usbpd-load.sh \
    file://cm33-usbpd-init \
    file://cm0-load.sh \
"
# v1.3.1 - same revision used by meta-st-stm32mp
SRCREV = "2f7258aa45e916777ffb4f6e1b5590f65304378d"

PACKAGES = "${PN}-cm33 ${PN}-cm0plus"

PV = "1.3.1"

USBPD_PROJECT_DIR = "${S}/Projects/STM32MP257F-DK/Demonstrations/USBPD_DRP_UCSI"
USBPD_FW_NAME    = "USBPD_DRP_UCSI_CM33_NonSecure_stripped.elf"

# https://wiki.st.com/stm32mpu/wiki/How_to_use_the_Cortex-M0%2B
CM0PLUS_PROJECT_DIR = "${S}/Projects/STM32MP257F-DK/Demonstrations/CM0PLUS_DEMO"
CM0PLUS_FW_NAME    = "CM0PLUS_DEMO_NonSecure_stripped.elf"

DEPENDS = "cmake-native gcc-arm-none-eabi-native"

inherit update-rc.d

INITSCRIPT_PACKAGES = "${PN}-cm33"
INITSCRIPT_NAME:${PN}-cm33 = "cm33-usbpd"
INITSCRIPT_PARAMS:${PN}-cm33 = "start 70 S . stop 30 0 6 ."

PACKAGECONFIG ??= ""

# cm33 trace uses usart6 (exposed on CN5 on dk) which may conflicts if linux uses it
PACKAGECONFIG[trace] = "-DTRACE=ON,-DTRACE=OFF"

do_configure() {
    cmake \
        -G "Unix Makefiles" \
        --fresh \
        -S ${USBPD_PROJECT_DIR} \
        -B ${USBPD_PROJECT_DIR}/build \
        ${EXTRA_OECMAKE}

    cmake \
        -G "Unix Makefiles" \
        --fresh \
        -S ${CM0PLUS_PROJECT_DIR} \
        -B ${CM0PLUS_PROJECT_DIR}/build \
        ${EXTRA_OECMAKE}
}

do_compile() {
    # fixes:
    # 
    # | CMake Error at CMakeLists.txt:1 (cmake_minimum_required):
    # |   Compatibility with CMake < 3.5 has been removed from CMake.
    # |
    # |   Update the VERSION argument <min> value.  Or, use the <min>...<max> syntax
    # |   to tell CMake that the project requires at least <min> but has been updated
    # |   to work with policies introduced by <max> or earlier.
    # |
    # |   Or, add -DCMAKE_POLICY_VERSION_MINIMUM=3.5 to try configuring anyway
    #
    export CMAKE_POLICY_VERSION_MINIMUM=3.5

    make -C ${USBPD_PROJECT_DIR}/build all
    make -C ${CM0PLUS_PROJECT_DIR}/build all
}

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware
    install -m 0644 \
        ${USBPD_PROJECT_DIR}/build/${USBPD_FW_NAME} \
        ${D}${nonarch_base_libdir}/firmware/

    install -m 0644 \
        ${CM0PLUS_PROJECT_DIR}/build/${CM0PLUS_FW_NAME} \
        ${D}${nonarch_base_libdir}/firmware/

    install -d ${D}${base_sbindir}
    install -m 0755 ${UNPACKDIR}/cm33-usbpd-load.sh ${D}${base_sbindir}/
    install -m 0755 ${UNPACKDIR}/cm0-load.sh ${D}${base_sbindir}/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/cm33-usbpd-init \
        ${D}${sysconfdir}/init.d/cm33-usbpd
}

FILES:${PN}-cm33 = " \
    ${nonarch_base_libdir}/firmware/${USBPD_FW_NAME} \
    ${base_sbindir}/cm33-usbpd-load.sh \
    ${sysconfdir}/init.d/cm33-usbpd \
"
FILES:${PN}-cm0plus = " \
    ${nonarch_base_libdir}/firmware/${CM0PLUS_FW_NAME} \
    ${base_sbindir}/cm0-load.sh \
"

COMPATIBLE_MACHINE = "^(mp2)$"

# The firmware is a bare-metal ARM32 (Cortex-M33) ELF inside an AArch64 package — expected.
INSANE_SKIP:${PN}-cm33 = "arch"
INSANE_SKIP:${PN}-cm0plus = "arch"
