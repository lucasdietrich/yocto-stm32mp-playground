DESCRIPTION = "udev rules for ble coprocessor ethernet interface over USB"
LICENSE = "CLOSED"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://ble-copro-usb-net.rules \
           file://configure-iface \
           "

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/ble-copro-usb-net.rules ${D}${sysconfdir}/udev/rules.d/ble-copro-usb-net.rules

    install -d ${D}${sysconfdir}/udev/scripts
    install -m 0755 ${WORKDIR}/configure-iface ${D}${sysconfdir}/udev/scripts/configure-iface
}