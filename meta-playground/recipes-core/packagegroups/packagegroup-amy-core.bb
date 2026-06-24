SUMMARY = "Amy core package group"
LICENSE = "CLOSED"

inherit packagegroup

# populate here
RDEPENDS:${PN} = "\
    ca-certificates \
    usb-gadget-ncm \
    tzdata \
    ntpd-start \
    dnsmasq \
    syslog-ng \
    logrotate \
    cronie \
    nginx \
    sqlite3 \
"
# nginx