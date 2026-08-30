SUMMARY = "Simple containers runtime script"
DESCRIPTION = "Setup a simple container at start"
LICENSE = "CLOSED"

SRC_URI = "file://run-containers.sh \
           file://run-containers.init"

INITSCRIPT_NAME = "run-containers"
INITSCRIPT_PARAMS = "defaults"

inherit update-rc.d

S = "${UNPACKDIR}"

CONTAINER_BASE_IMAGE_NAME := "example-container-image"

RDEPENDS:${PN} += "${VIRTUAL-RUNTIME_container_runtime}"

do_install:append() {
    # copy the startup script
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/run-containers.sh ${D}${bindir}/run-containers.sh

    # copy the init script
    install -d ${D}${sysconfdir}/init.d
    install -m 755 ${UNPACKDIR}/run-containers.init "${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}"

    # copy the container base image
    install -d ${D}/containers
    install -m 0644 ${DEPLOY_DIR_IMAGE}/${CONTAINER_BASE_IMAGE_NAME}.img ${D}/containers/${CONTAINER_BASE_IMAGE_NAME}.img
}

# This package depends on the container base image
do_install[depends] += "${CONTAINER_BASE_IMAGE_NAME}:do_image_complete"

FILES:${PN} += "/containers/* \
                ${bindir}/run-containers.sh"
