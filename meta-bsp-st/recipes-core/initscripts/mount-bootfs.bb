SUMMARY = "Mount bootfs partition by partlabel"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://mount-bootfs"

S = "${UNPACKDIR}"

inherit update-rc.d

INITSCRIPT_NAME = "mount-bootfs"
INITSCRIPT_PARAMS = "start 05 S . stop 20 0 6 ."

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/mount-bootfs ${D}${sysconfdir}/init.d/mount-bootfs
}