SUMMARY = "Trusted Firmware A Tools"

require tf-a-common.inc

DEPENDS = "dtc openssl-native"
RDEPENDS:${PN} = "libcrypto"

do_compile() {
    # fiptool's Makefile hardcodes OPENSSL_DIR=/usr, so point it at our sysroot.
    # ${prefix} is already the full staging path for native/nativesdk recipes.
    oe_runmake -C ${S} PLAT=stm32mp2 OPENSSL_DIR=${prefix} fiptool
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/tools/fiptool/fiptool ${D}${bindir}/
}

COMPATIBLE_MACHINE = "^$"
BBCLASSEXTEND += "native nativesdk"
