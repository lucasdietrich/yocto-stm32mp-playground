SUMMARY = "Miscellaneous utilities for the AMY platform"
LICENSE = "CLOSED"

SRC_URI = "file://probe-gcm.c \
           file://probe-gcm-setkey.c \
           file://set-crypto-priority.c \
        "

S = "${UNPACKDIR}"

PACKAGECONFIG ??= "probe-gcm probe-gcm-setkey set-crypto-priority"
PACKAGECONFIG[probe-gcm] = ""
PACKAGECONFIG[probe-gcm-setkey] = ""
PACKAGECONFIG[set-crypto-priority] = ""

do_compile() {
    for program in ${PACKAGECONFIG}; do
        ${CC} ${CFLAGS} ${LDFLAGS} -fdebug-prefix-map=${WORKDIR}= -o ${WORKDIR}/${program} ${UNPACKDIR}/${program}.c
    done
}

do_install() {
    install -d ${D}${bindir}
    for program in ${PACKAGECONFIG}; do
        install -m 0755 ${WORKDIR}/${program} ${D}${bindir}/${program}
    done
}

FILES:${PN} += "${bindir}/"