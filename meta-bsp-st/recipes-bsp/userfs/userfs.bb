SUMMARY = "userfs is a tool to create/manage user partitions and filesystems"
HOMEPAGE = "https://github.com/lucasdietrich/userfs"
LICENSE = "CLOSED"

SRC_URI = "git://github.com/lucasdietrich/userfs.git;protocol=https;branch=feature/gpt \
           file://init-user-fs.sh \
           file://factory_reset.sh \
           "
SRCREV = "727dbad4ff7dcdea42fecd06ad5b0bc3725bf7b6"

DEPENDS += "util-linux"
RDEPENDS:${PN} += "util-linux-libfdisk libubootenv btrfs-tools parted"

inherit meson pkgconfig update-rc.d

# create overlay for /opt
EXTRA_OEMESON += "-Dpartition_table=gpt"
EXTRA_OEMESON += "-Doverlay_opt=true"
EXTRA_OEMESON += "-Ddefault_block_device_name=/dev/mmcblk0"

S = "${WORKDIR}/git"

# the filesystems must be created after the `mountall.sh` script,
# before any application want to store data
INITSCRIPT_PARAMS = "start 04 S ."
INITSCRIPT_NAME = "${PN}.sh"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-user-fs.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    install -m 0755 ${WORKDIR}/factory_reset.sh ${D}${bindir}/factory_reset
}