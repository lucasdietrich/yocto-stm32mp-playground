SUMMARY = "Partition clone tool"
DESCRIPTION = "Utilities to backup and restore used blocks on a partition, \
               using existing filesystem libraries for compatibility."
HOMEPAGE = "https://partclone.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRC_URI = "git://github.com/Thomas-Tsai/partclone.git;protocol=https;branch=master"
SRCREV = "198be487eecf816caac138587c68989c9aac8ac4"

DEPENDS = "e2fsprogs xxhash zstd"

inherit autotools pkgconfig gettext

# Optional filesystem support - enable what you need
PACKAGECONFIG ??= "ext2 btrfs"
PACKAGECONFIG[ext2]  = "--enable-extfs,  --disable-extfs,  e2fsprogs"
PACKAGECONFIG[xfs]   = "--enable-xfs,    --disable-xfs,    xfsprogs"
PACKAGECONFIG[btrfs] = "--enable-btrfs,  --disable-btrfs,  btrfs-tools"
PACKAGECONFIG[fat]   = "--enable-fat,    --disable-fat,    "
PACKAGECONFIG[exfat] = "--enable-exfat,  --disable-exfat,  exfatprogs"

# Disable ncursesw unless you want the interactive TUI
EXTRA_OECONF = "--disable-ncursesw"

# Skip docs subdir: man pages require xsltproc/DocBook and are not needed on target
do_install() {
    oe_runmake DESTDIR="${D}" SUBDIRS="po src" install
    rm -rf ${D}${datadir}/bash-completion
}