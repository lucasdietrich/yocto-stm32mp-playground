require linux.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files/6.18:"

SRC_URI += "file://0001-Add-custom-amy-devicetree.patch"

PV = "6.18"
SRC_URI[sha256sum] = "9106a4605da9e31ff17659d958782b815f9591ab308d03b0ee21aad6c7dced4b"

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"