FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://0001-Add-x1025-as-valid-JLINK-PID-nrf52840dk-board.patch;patchdir=src/jtag/drivers/libjaylink"

# enable jlink support
EXTRA_OECONF += "--enable-internal-libjaylink --enable-jlink"

# upgrade libjaylink from 0.2.0 to 0.4.0
SRCREV_libjaylink = "fa52ee261ba39f9806ac7cfa658d4f231132ab4a"
