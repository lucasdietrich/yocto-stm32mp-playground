SUMMARY = "OP-TEE Client"
HOMEPAGE = "https://optee.readthedocs.io/en/latest/building/gits/optee_client.html"

LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=69663ab153298557a59c67a60a743e5b"

S = "${WORKDIR}/git"

SRC_BRANCH ??= "master"
SRC_URI = "git://github.com/OP-TEE/optee_client.git;protocol=https;branch=${SRC_BRANCH}"
SRCREV = "9f5e90918093c1d1cd264d8149081b64ab7ba672"
PV = "4.10.0"

DEPENDS += "util-linux-libuuid"

inherit update-rc.d cmake pkgconfig

SRC_URI += "file://tee-supplicant.init"

EXTRA_OECMAKE += " \
    -DBUILD_SHARED_LIBS=ON \
    -DCFG_TEE_FS_PARENT_PATH='/mnt/tee-fs' \
    "

INITSCRIPT_NAME = "tee-supplicant"
INITSCRIPT_PARAMS = "start 10 S . stop 90 0 6 ."

inherit useradd

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system tee; --system teepriv"

do_install:append() {
    # TODO
    rm -rf ${D}/${libdir}/systemd

    # install init script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/tee-supplicant.init ${D}${sysconfdir}/init.d/tee-supplicant
}