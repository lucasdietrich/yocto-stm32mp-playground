FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://custom-args.sh \
            file://hwrevision.in"

wwwdir = "/www/swupdate"

do_install:append() {

    # Substitute MACHINE and DISTRO_VERSION into hwrevision
    sed -e 's/@MACHINE@/${MACHINE}/g' \
        -e 's/@DISTRO_VERSION@/${DISTRO_VERSION}/g' \
        ${WORKDIR}/hwrevision.in > ${D}${sysconfdir}/hwrevision

    # Install custom arguments script
    install -d ${D}${sysconfdir}/swupdate/conf.d
    install -m 0644 ${WORKDIR}/custom-args.sh ${D}${sysconfdir}/swupdate/conf.d/custom-args.sh
}