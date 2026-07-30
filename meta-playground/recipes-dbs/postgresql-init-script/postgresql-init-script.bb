DESCRIPTION = "Init script for PostgreSQL"
LICENSE = "CLOSED"

SRC_URI = "file://init-postgresql.sh \
           file://postgresql-init.service \
           "

# RDEPENDS:${PN} += "bash"
DEPENDS:append = "update-rc.d-native"

inherit systemd

SYSTEMD_SERVICE:${PN} = "postgresql-init.service"

do_install:append() {
    install -d 644 ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-postgresql.sh ${D}${sysconfdir}/init.d/init-postgresql.sh

    # start order must be lower than postgresql init script
    update-rc.d -r ${D} init-postgresql.sh start 63 2 3 4 5 .

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/postgresql-init.service ${D}${systemd_system_unitdir}/postgresql-init.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for postgresql-init.service to call
    install -d ${D}${libdir}/postgresql-init-script
    install -m 0755 ${WORKDIR}/init-postgresql.sh ${D}${libdir}/postgresql-init-script/postgresql-init
}

FILES_${PN} += "${sysconfdir}/init.d ${libdir}/postgresql-init-script/postgresql-init"