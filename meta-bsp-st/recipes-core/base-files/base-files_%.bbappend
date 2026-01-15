FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_configure:append () {
    # Substitute MACHINE and DISTRO_VERSION into hwrevision
    sed -e 's/@MACHINE@/${MACHINE}/g' \
        -e 's/@DISTRO_VERSION@/${DISTRO_VERSION}/g' \
        -i ${WORKDIR}/motd
}