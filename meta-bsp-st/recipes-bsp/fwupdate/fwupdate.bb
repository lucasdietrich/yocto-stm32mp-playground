SUMMARY = "fwupdate script are used to update the firmware of the Raspberry Pi"
LICENSE = "CLOSED"

SRC_URI = "file://init-fwupdate.sh \
           file://fwupdate.sh \
           file://detect-booted-rootfs.sh \
           file://fwupdate.service \
           "

inherit update-rc.d systemd

RDEPENDS:${PN} = "\
    u-boot-fw-utils \
    u-boot-env \
"

# starts before swupdate
INITSCRIPT_PARAMS = "start 65 S ."
INITSCRIPT_NAME = "${PN}.sh"

SYSTEMD_SERVICE:${PN} = "fwupdate.service"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-fwupdate.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/fwupdate.sh ${D}${bindir}/fwupdate

    install -d ${D}${libdir}/fwupdate
    install -m 0644 ${WORKDIR}/detect-booted-rootfs.sh ${D}${libdir}/fwupdate/detect-booted-rootfs.sh

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/fwupdate.service ${D}${systemd_system_unitdir}/fwupdate.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for fwupdate.service to call
    install -m 0755 ${WORKDIR}/init-fwupdate.sh ${D}${libdir}/fwupdate/fwupdate-init.sh
}

FILES:${PN} += "${bindir}/fwupdate ${libdir}/fwupdate/detect-booted-rootfs.sh ${libdir}/fwupdate/fwupdate-init.sh"