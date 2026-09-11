SUMMARY = "Example LED button application"
LICENSE = "CLOSED"

SRC_URI = "file://example-led-button.c"

S = "${UNPACKDIR}"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${WORKDIR}/example-led-button ${S}/example-led-button.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/example-led-button ${D}${bindir}/example-led-button
}

FILES:${PN} += "${bindir}/"