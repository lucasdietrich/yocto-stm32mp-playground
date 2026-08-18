SUMMARY = "fwupdate script are used to update the firmware of the Raspberry Pi"
LICENSE = "CLOSED"

SRC_URI = "file://init-fwupdate.sh \
           file://fwupdate.sh \
           file://detect-booted-rootfs.sh \
           "

S = "${UNPACKDIR}"

inherit update-rc.d

RDEPENDS:${PN} = "\
    u-boot-fw-utils \
    u-boot-env \
"

# starts before swupdate
INITSCRIPT_PARAMS = "start 65 S ."
INITSCRIPT_NAME = "${PN}.sh"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/init-fwupdate.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/fwupdate.sh ${D}${bindir}/fwupdate

    install -d ${D}${libdir}/fwupdate
    install -m 0644 ${UNPACKDIR}/detect-booted-rootfs.sh ${D}${libdir}/fwupdate/detect-booted-rootfs.sh
}

FILES:${PN} += "${bindir}/fwupdate ${libdir}/fwupdate/detect-booted-rootfs.sh"