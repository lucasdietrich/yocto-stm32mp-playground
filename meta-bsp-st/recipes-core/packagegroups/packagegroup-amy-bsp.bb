SUMMARY = "Amy BSP package group"
LICENSE = "CLOSED"

inherit packagegroup

# populate here
RDEPENDS:${PN} = "\
    userfs \
    os-release \
"

# u-boot-fw-utils add the fw_printenv fw_setenv utilities
# TODO ADD u-boot-env ???? u-boot-env provides the env config file `/etc/fw_env.config`
RDEPENDS:${PN} += "\
    u-boot-fw-utils \
    u-boot-env \
"

RDEPENDS:${PN} += "\
    mount-bootfs \
"