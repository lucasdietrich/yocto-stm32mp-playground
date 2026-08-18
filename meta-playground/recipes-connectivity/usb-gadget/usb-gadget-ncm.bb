SUMMARY = "USB CDC-NCM gadget setup"
DESCRIPTION = "Installs configfs USB NCM gadget setup script and udev rules for usb0 configuration"
LICENSE = "CLOSED"

inherit update-rc.d

SRC_URI = " \
    file://usb-gadget-ncm \
    file://usb-ncm-iface \
    file://90-usb-ncm.rules \
    file://init-usb-gadget-ncm \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} += " \
    iproute2 \
    udev \
"

INITSCRIPT_NAME = "usb-gadget-ncm"
INITSCRIPT_PARAMS = "start 40 S . stop 40 0 6 ."

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${UNPACKDIR}/usb-gadget-ncm ${D}${sbindir}/usb-gadget-ncm
    install -m 0755 ${UNPACKDIR}/usb-ncm-iface ${D}${sbindir}/usb-ncm-iface

    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/90-usb-ncm.rules ${D}${sysconfdir}/udev/rules.d/90-usb-ncm.rules
}

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${UNPACKDIR}/usb-gadget-ncm ${D}${sbindir}/usb-gadget-ncm
    install -m 0755 ${UNPACKDIR}/usb-ncm-iface ${D}${sbindir}/usb-ncm-iface

    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/90-usb-ncm.rules ${D}${sysconfdir}/udev/rules.d/90-usb-ncm.rules

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/init-usb-gadget-ncm ${D}${sysconfdir}/init.d/usb-gadget-ncm
}

FILES:${PN} += " \
    ${sbindir}/usb-gadget-ncm \
    ${sbindir}/usb-ncm-iface \
    ${sysconfdir}/udev/rules.d/90-usb-ncm.rules \
    ${sysconfdir}/init.d/usb-gadget-ncm \
"