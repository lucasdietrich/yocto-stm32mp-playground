DESCRIPTION = "Init script for PostgreSQL"
LICENSE = "CLOSED"

SRC_URI = "file://init-postgresql.sh"

# RDEPENDS:${PN} += "bash"
DEPENDS:append = "update-rc.d-native"

do_install:append() {
    install -d 644 ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-postgresql.sh ${D}${sysconfdir}/init.d/init-postgresql.sh

    # start order must be lower than postgresql init script
    update-rc.d -r ${D} init-postgresql.sh start 63 2 3 4 5 .
}

FILES_${PN} += "${sysconfdir}/init.d"