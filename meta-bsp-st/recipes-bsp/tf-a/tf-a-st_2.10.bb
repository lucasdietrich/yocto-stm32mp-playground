require tf-a.inc

SRC_URI = "git://github.com/TrustedFirmware-A/trusted-firmware-a.git;protocol=https;branch=lts-v2.10"
SRCREV = "b1f575090608cf378440f35e7f973ee9ee0ea182"
PV = "2.10.13"

FILESEXTRAPATHS:prepend := "${THISDIR}/files-st:"

SRC_URI += " \
    https://raw.githubusercontent.com/STMicroelectronics/meta-st-stm32mp/refs/heads/scarthgap/recipes-bsp/trusted-firmware-a/tf-a-stm32mp/0001-v2.10-stm32mp-r2.patch \
    file://0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch \
    "
SRC_URI[sha256sum] = "9db38a8148c080b873862fd3509db8443a503f3958c038aeefcf6f0f0198bf4e"

TFA_DEVICETREE ?= "stm32mp157f-dk2"