SUMMARY = "USB CDC-NCM gadget setup"
DESCRIPTION = "Installs configfs USB NCM gadget setup script and udev rules for usb0 configuration"
LICENSE = "CLOSED"

inherit update-rc.d systemd

SRC_URI = " \
    file://usb-gadget-ncm \
    file://usb-ncm-iface \
    file://90-usb-ncm.rules \
    file://init-usb-gadget-ncm \
    file://usb-gadget-ncm.service \
"

S = "${WORKDIR}"

RDEPENDS:${PN} += " \
    iproute2 \
    udev \
"

INITSCRIPT_NAME = "usb-gadget-ncm"
INITSCRIPT_PARAMS = "start 40 S . stop 40 0 6 ."

SYSTEMD_SERVICE:${PN} = "usb-gadget-ncm.service"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/usb-gadget-ncm ${D}${sbindir}/usb-gadget-ncm
    install -m 0755 ${WORKDIR}/usb-ncm-iface ${D}${sbindir}/usb-ncm-iface

    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/90-usb-ncm.rules ${D}${sysconfdir}/udev/rules.d/90-usb-ncm.rules

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-usb-gadget-ncm ${D}${sysconfdir}/init.d/usb-gadget-ncm

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/usb-gadget-ncm.service ${D}${systemd_system_unitdir}/usb-gadget-ncm.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for usb-gadget-ncm.service to call
    install -d ${D}${libdir}/usb-gadget-ncm
    install -m 0755 ${WORKDIR}/init-usb-gadget-ncm ${D}${libdir}/usb-gadget-ncm/usb-gadget-ncm-init
}

FILES:${PN} += " \
    ${sbindir}/usb-gadget-ncm \
    ${sbindir}/usb-ncm-iface \
    ${sysconfdir}/udev/rules.d/90-usb-ncm.rules \
    ${sysconfdir}/init.d/usb-gadget-ncm \
    ${libdir}/usb-gadget-ncm/usb-gadget-ncm-init \
"