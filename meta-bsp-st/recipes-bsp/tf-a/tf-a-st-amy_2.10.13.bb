require tf-a-st_2.10.13.bb

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch \
    "