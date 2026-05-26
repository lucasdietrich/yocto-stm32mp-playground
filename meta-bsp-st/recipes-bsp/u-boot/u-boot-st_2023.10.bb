require u-boot.inc

COMPATIBLE_MACHINE = "(mp2)"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/2023.10:"

SRCREV  = "4459ed60cb1e0562bc5b40405e2b4b9bbf766d57"

SRC_URI += "https://raw.githubusercontent.com/STMicroelectronics/meta-st-stm32mp/bcadba4d92cbfeba7b7a876c2e828f37a70d0d0a/recipes-bsp/u-boot/u-boot-stm32mp/0001-v2023.10-stm32mp-r3.patch"
SRC_URI[sha256sum] = "3f25e94894a0bccfb5bdcbce0ac7012cde50a1c39a5caab30a5f4e5345a13c43"