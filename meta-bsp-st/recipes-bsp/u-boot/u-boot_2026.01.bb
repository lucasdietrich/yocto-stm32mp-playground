require u-boot.inc

COMPATIBLE_MACHINE = "(mp1|mp2)"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/2026.01:"

SRCREV = "127a42c7257a6ffbbd1575ed1cbaa8f5408a44b3"

# specific to mp1
SRC_URI:append:mp1 = " \
    file://0001-Disable-SCMI-for-stm32mp157f-dk2.patch \
    file://0002-Change-custom-boot-command.patch \
    "