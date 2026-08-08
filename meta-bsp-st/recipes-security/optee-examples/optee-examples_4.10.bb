SUMMARY = "OP-TEE example Trusted Applications and host apps"
DESCRIPTION = "Sample Trusted Applications and companion host applications \
demonstrating the OP-TEE Client API (optee_examples)."

HOMEPAGE = "https://github.com/linaro-swg/optee_examples"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=cd95ab417e23b94f381dafc453d70c30"

SRC_URI = "git://github.com/linaro-swg/optee_examples.git;protocol=https;branch=master"
SRCREV = "934c7edb74a26e90f68024cf441073528444177f"

PACKAGE_ARCH = "${MACHINE_ARCH}"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

DEPENDS = "virtual/optee-os optee-client"

TA_DEV_KIT_DIR = "${RECIPE_SYSROOT}${SYSROOT_OPTEE_DIR_BASE}/export-user_ta"

# Point the build at the TA devkit staged into this recipe's sysroot
# by the optee-os -dev (or -ta-devkit) package via DEPENDS above.
EXTRA_OEMAKE = " \
    TA_DEV_KIT_DIR=${TA_DEV_KIT_DIR} \
    HOST_CROSS_COMPILE=${HOST_PREFIX} \
    TA_CROSS_COMPILE=${HOST_PREFIX} \
    TEEC_EXPORT=${STAGING_DIR_HOST}${prefix} \
    OUTPUT_DIR=${B} \
"

CFLAGS += "--sysroot=${STAGING_DIR_HOST}"

do_compile() {
    oe_runmake -C ${S} ${EXTRA_OEMAKE}
}
do_compile[cleandirs] = "${B}"

do_install() {
    mkdir -p ${D}${nonarch_base_libdir}/optee_armtz
    mkdir -p ${D}${bindir}
    mkdir -p ${D}${libdir}/tee-supplicant/plugins
    install -D -p -m 0755 ${B}/ca/* ${D}${bindir}
    install -D -p -m 0644 ${B}/ta/*.ta ${D}${nonarch_base_libdir}/optee_armtz
    install -D -p -m 0644 ${B}/plugins/* ${D}${libdir}/tee-supplicant/plugins
}


FILES:${PN} += "${nonarch_base_libdir}/optee_armtz/ \
                ${libdir}/tee-supplicant/plugins/ \
               "
