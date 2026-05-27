require u-boot.inc

COMPATIBLE_MACHINE = "(mp1|mp2)"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/2026.04:"

SRCREV = "88dc2788777babfd6322fa655df549a019aa1e69"

# specific to mp1
SRC_URI:append:mp1 = " \
    file://0001-Disable-SCMI-for-stm32mp157f-dk2.patch \
    "

SRC_URI:append:mp2 = " \
    file://0002-stm32mp2-Add-default-value-for-DEBUG_UART_CLOCK.patch \
    "