require linux-amy.inc

COMPATIBLE_MACHINE = "^(mp2)$"

PV = "6.6.116"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/6.6.116-st:"

SRC_URI += " \
    https://raw.githubusercontent.com/STMicroelectronics/meta-st-stm32mp/bcadba4d92cbfeba7b7a876c2e828f37a70d0d0a/recipes-kernel/linux/linux-stm32mp/6.6/6.6.116/0001-v6.6-stm32mp-r3.patch;name=0001-v6.6-stm32mp-r3.patch \
    file://amy-stm32mp2_defconfig \
"

SRC_URI[sha256sum] = "739016a981571d0cd3874f2112e25c55eb2fd2ac6d88ef1a36e346bc1f7233a0"
SRC_URI[0001-v6.6-stm32mp-r3.patch.sha256sum] = "a726aa312d11866f5242961c97ac9fd4a3165e9eb35efd655f5c7855cdd84287"


do_configure:prepend() {
    cp ${WORKDIR}/amy-stm32mp2_defconfig defconfig
}

# "transitional" keyword introduced in kernel 6.18 is not yet supported 
# by python3-kconfiglib maintained by the Zephyr project. 
# see: https://github.com/zephyrproject-rtos/Kconfiglib/issues/31
do_kernel_configcheck[noexec] = "1"