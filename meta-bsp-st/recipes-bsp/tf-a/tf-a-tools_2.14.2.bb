SUMMARY = "Trusted Firmware A Tools"

require tf-a-common.inc
require tf-a-2.14.2.inc

DEPENDS = "dtc"

do_compile() {
    # TODO target stm32mp2 for now
    oe_runmake -C ${S} PLAT=stm32mp2 fiptool
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/tools/fiptool/fiptool ${D}${bindir}/
}

COMPATIBLE_MACHINE = "^$"
BBCLASSEXTEND += "native nativesdk"