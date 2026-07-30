SUMMARY = "userfs is a tool to create/manage user partitions and filesystems"
HOMEPAGE = "https://github.com/lucasdietrich/userfs"
LICENSE = "CLOSED"

SRC_BRANCH ?= "mpx"
SRC_URI = "git://github.com/lucasdietrich/userfs.git;protocol=https;branch=${SRC_BRANCH} \
           file://init-user-fs.sh \
           file://factory_reset.sh \
           file://userfs.service \
           "
SRCREV = "2fd863904559da870b4d1fab1c77ecf8116a922c"

PACKAGECONFIG ??= "swap"
PACKAGECONFIG[swap] = ",,,swapfile"

# TODO use e2fsprogs
DEPENDS += "util-linux libdevmapper"
RDEPENDS:${PN} += "util-linux-libfdisk btrfs-tools parted libubootenv libdevmapper"

inherit meson pkgconfig update-rc.d systemd

# create overlay for /opt
EXTRA_OEMESON += "-Dpartition_table=gpt"
EXTRA_OEMESON += "-Doverlay_opt=true"
EXTRA_OEMESON += "-Ddefault_block_device_name=/dev/mmcblk0"
EXTRA_OEMESON += "-Dteefs=true"
EXTRA_OEMESON += "-Dmanufacturer_partition=true"

S = "${WORKDIR}/git"

# the filesystems must be created after the `mountall.sh` script,
# before any application want to store data
INITSCRIPT_PARAMS = "start 04 S ."
INITSCRIPT_NAME = "${PN}.sh"

SYSTEMD_SERVICE:${PN} = "userfs.service"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/init-user-fs.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    install -m 0755 ${WORKDIR}/factory_reset.sh ${D}${bindir}/factory_reset

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/userfs.service ${D}${systemd_system_unitdir}/userfs.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for userfs.service to call
    install -d ${D}${libdir}/userfs
    install -m 0755 ${WORKDIR}/init-user-fs.sh ${D}${libdir}/userfs/userfs.sh
}

FILES:${PN} += "${libdir}/userfs/userfs.sh"