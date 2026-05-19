require tf-a.inc

SRC_URI = "git://github.com/TrustedFirmware-A/trusted-firmware-a.git;protocol=https;branch=lts-v2.10"
SRCREV = "b1f575090608cf378440f35e7f973ee9ee0ea182"
PV = "2.10.13"

FILESEXTRAPATHS:prepend := "${THISDIR}/files-st:"

SRC_URI += " \
    https://raw.githubusercontent.com/STMicroelectronics/meta-st-stm32mp/bcadba4d92cbfeba7b7a876c2e828f37a70d0d0a/recipes-bsp/trusted-firmware-a/tf-a-stm32mp/0001-v2.10-stm32mp-r3.patch \
    file://0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch \
    "
SRC_URI[sha256sum] = "60dd08aa5c0b01fe77133827caa969c3aad84eaa040fa2546f0890ea45227447"