SUMMARY = "Mount bootfs partition by partlabel"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://mount-bootfs \
           file://mount-bootfs.service \
           "

S = "${WORKDIR}"

inherit update-rc.d systemd

INITSCRIPT_NAME = "mount-bootfs"
INITSCRIPT_PARAMS = "start 05 S . stop 20 0 6 ."

SYSTEMD_SERVICE:${PN} = "mount-bootfs.service"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/mount-bootfs ${D}${sysconfdir}/init.d/mount-bootfs

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/mount-bootfs.service ${D}${systemd_system_unitdir}/mount-bootfs.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for mount-bootfs.service to call
    install -d ${D}${libdir}/mount-bootfs
    install -m 0755 ${WORKDIR}/mount-bootfs ${D}${libdir}/mount-bootfs/mount-bootfs
}

FILES:${PN} += "${libdir}/mount-bootfs/mount-bootfs"