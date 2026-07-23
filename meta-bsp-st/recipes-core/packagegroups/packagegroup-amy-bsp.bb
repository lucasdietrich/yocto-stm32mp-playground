SUMMARY = "Amy BSP package group"
LICENSE = "CLOSED"

inherit packagegroup

RDEPENDS:${PN} = "\
    userfs \
    swapfile \
    os-release \
    udev \
    lvm2 \
    cryptsetup \
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

# optee related
RDEPENDS:${PN} += "\
    optee-client \
    optee-examples \
"

# swupdate dependencies
RDEPENDS:${PN} += "\
    fwupdate \
    swupdate \
    swupdate-www \
    swupdate-tools \
"

# swupdate dependencies
RDEPENDS:${PN}:append:mp2 = "\
    stm32mp2-cm33-fw \
"

RDEPENDS:${PN} += "${@bb.utils.contains('AMY_DEBUG_UTILS', '1', '\
    partclone \
    zstd \
    mmc-utils \
    iptables \
', '', d)}"

# fsbackup
# fsbench