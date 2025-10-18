SUMMARIZE = "tool to generate multiple filesystem and flash images from a tree"
HOMEPAGE = "https://github.com/pengutronix/genimage/tree/master"
LICENSE = "GPL-2.0-only"

LIC_FILES_CHKSUM = "file://COPYING;md5=570a9b3749dd0463a1778803b12a6dce"

SRC_URI = "git://github.com/pengutronix/genimage.git;protocol=https;branch=master"
SRCREV = "67a399476add05f29ef1ad722856728fb7639ba9"

S = "${WORKDIR}/git"

inherit autotools pkgconfig

DEPENDS = "libconfuse"

BBCLASSEXTEND = "native nativesdk"