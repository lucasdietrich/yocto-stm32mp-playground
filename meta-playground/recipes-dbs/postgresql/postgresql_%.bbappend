INITSCRIPT_PARAMS = "start 64 1 2 3 4 5 . stop 64 0 6 ."

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://postgresql.conf"

RDEPENDS:${PN} += "postgresql-init-script"

do_install:append() {
    install -d ${D}${sysconfdir}/postgresql
    install -m 0644 ${WORKDIR}/postgresql.conf ${D}${sysconfdir}/postgresql/postgresql.conf
}