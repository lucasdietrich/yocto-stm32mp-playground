FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://caniot-logrotate"

RDEPENDS:${PN} += "logrotate"

do_install:append() {
    # Install logrotate configuration for caniot controller logs
    install -d ${D}${sysconfdir}/logrotate.d
    install -m 0644 ${WORKDIR}/caniot-logrotate ${D}${sysconfdir}/logrotate.d/caniot-controller
}