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

inherit update-rc.d systemd cmake pkgconfig

SRC_URI += "file://tee-supplicant.init"
SRC_URI += "file://tee-supplicant.service"

EXTRA_OECMAKE += " \
    -DBUILD_SHARED_LIBS=ON \
    -DCFG_TEE_FS_PARENT_PATH='/mnt/tee-fs' \
    "

INITSCRIPT_NAME = "tee-supplicant"
INITSCRIPT_PARAMS = "start 10 S . stop 90 0 6 ."

SYSTEMD_SERVICE:${PN} = "tee-supplicant.service"

inherit useradd

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system tee; --system teepriv"

do_install:append() {
    # the upstream cmake build ships its own systemd unit (tee-supplicant@.service)
    # which does not match our fixed (non-templated) init script/ARGS, drop it and
    # install our own tee-supplicant.service below instead
    rm -rf ${D}/${libdir}/systemd

    # install init script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/tee-supplicant.init ${D}${sysconfdir}/init.d/tee-supplicant

    # install systemd unit
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/tee-supplicant.service ${D}${systemd_system_unitdir}/tee-supplicant.service

    # the systemd bbclass deletes ${sysconfdir}/init.d on pure-systemd builds
    # (DISTRO_FEATURES has systemd but not sysvinit), so keep a second copy of
    # the same script outside init.d for tee-supplicant.service to call
    install -d ${D}${libdir}/optee-client
    install -m 0755 ${WORKDIR}/tee-supplicant.init ${D}${libdir}/optee-client/tee-supplicant-init
}

FILES:${PN} += "${libdir}/optee-client/tee-supplicant-init"