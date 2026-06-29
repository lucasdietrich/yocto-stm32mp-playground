SUMMARY = "Amy BSP package group"
LICENSE = "CLOSED"

inherit packagegroup

RDEPENDS:${PN} = "\
    userfs \
    swapfile \
    os-release \
    udev \
"

# u-boot-fw-utils add the fw_printenv fw_setenv utilities
# TODO ADD u-boot-env ???? u-boot-env provides the env config file `/etc/fw_env.config`
RDEPENDS:${PN} += "\
    u-boot-fw-utils \
    u-boot-env \
"

# mount /boot at boot time
RDEPENDS:${PN} += "\
    mount-bootfs \
"

# swupdate dependencies
RDEPENDS:${PN} += "\
    fwupdate \
    swupdate \
    swupdate-www \
    swupdate-tools \
"

# swupdate dependencies
RDEPENDS:${PN} += "\
    stm32mp2-cm33-fw \
"