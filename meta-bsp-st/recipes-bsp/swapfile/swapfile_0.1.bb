SUMMARY = "swapfile is a tool to create swapfiles"
LICENSE = "CLOSED"

SRC_URI = "file://swapfile.c"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${WORKDIR}/swapfile ${WORKDIR}/swapfile.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/swapfile ${D}${bindir}/swapfile
}

FILES:${PN} += "${bindir}/"