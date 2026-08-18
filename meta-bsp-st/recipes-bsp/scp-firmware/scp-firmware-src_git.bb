SUMMARY = "SCP-firmware source tree for STM32MP2 OP-TEE SCMI integration"
DESCRIPTION = "Provides the SCP-firmware 2.16.0 source tree for use by OP-TEE \
as an embedded SCMI server (CFG_SCMI_SCPFW). No standalone build is performed."

LICENSE = "BSD-3-Clause & Apache-2.0"
LIC_FILES_CHKSUM = "file://license.md;beginline=5;md5=9db9e3d2fb8d9300a6c3d15101b19731 \
                    file://contrib/cmsis/git/LICENSE.txt;md5=e3fc50a88d0a364313df4b21ef20c29e"

COMPATIBLE_MACHINE = "(mp2)"
PACKAGE_ARCH = "${MACHINE_ARCH}"

PV = "2.16.0+git"

SRC_URI = "gitsm://git.gitlab.arm.com/firmware/SCP-firmware.git;protocol=https;branch=main \
          "
# v2.14.0 tag (3267f296) lacks product/optee/stm32mp2; use post-2.16.0 main
SRCREV = "f76402dfefc7557b97aac6226c0b1302e2dc5e12"

SCP_FIRMWARE_SYSROOT_DIR ??= "/usr/src/scp-firmware"

# No compilation — we only provide the source tree
do_configure[noexec] = "1"
do_compile[noexec] = "1"

export_binaries() {
    local destdir="$1"
    install -d ${destdir}
    cp -r ${S}/. ${destdir}/
}

scp_firmware_sysroot_populate() {
  export_binaries ${SYSROOT_DESTDIR}${SCP_FIRMWARE_SYSROOT_DIR}
}
SYSROOT_PREPROCESS_FUNCS =+ "scp_firmware_sysroot_populate"

# Make the source tree available in the sysroot consumed by optee-os
SYSROOT_DIRS:append = " ${SCP_FIRMWARE_SYSROOT_DIR}"