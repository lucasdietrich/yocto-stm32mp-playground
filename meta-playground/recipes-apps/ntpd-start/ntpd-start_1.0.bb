SUMMARY = "Script to start ntpd client service"

LICENSE = "CLOSED"

SRC_URI = "file://ntpd \
           file://ntpd.service \
           "

inherit update-rc.d systemd

# should be run just after dhcp has been resolved
INITSCRIPT_PARAMS = "defaults 21"
INITSCRIPT_NAME = "ntpd"

SYSTEMD_SERVICE:${PN} = "ntpd.service"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/ntpd ${D}${sysconfdir}/init.d/ntpd

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/ntpd.service ${D}${systemd_system_unitdir}/ntpd.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for ntpd.service to call
    install -d ${D}${libdir}/ntpd-start
    install -m 0755 ${WORKDIR}/ntpd ${D}${libdir}/ntpd-start/ntpd-init
}

FILES:${PN} += "${libdir}/ntpd-start/ntpd-init"