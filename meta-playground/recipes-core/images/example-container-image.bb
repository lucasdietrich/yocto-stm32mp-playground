require recipes-extended/images/container-base.bb

DESCRIPTION = "Example container (base) image"

# tar output is not required
OCI_IMAGE_TAR_OUTPUT = "false"

IMAGE_FSTYPES = "container oci"

CONTAINER_SHELL = "busybox"

# IMAGE_CONTAINER_NO_DUMMY

IMAGE_INSTALL += " \
    os-release \
    busybox \
    libgcc \
    libcap \
"

# debug tools
IMAGE_INSTALL += " \
    libcap-bin \
    strace \
    uftrace \
    perf \
    tcpdump \
"

OCI_IMAGE_AUTHOR = "Lucas"
OCI_IMAGE_ENV_VARS = "\
    AMY_CONTAINER=1 \
"

OCI_IMAGE_TAG = "easy"
CONTAINER_SHELL = "busybox"
# OCI_IMAGE_ENTRYPOINT = "curl"
# OCI_IMAGE_ENTRYPOINT_ARGS = "http://localhost:80"

DEPENDS += "umoci-native \
            skopeo-native \
            erofs-utils-native \
            "

rootfs_add_empty_resolv_conf() {
    install -d ${IMAGE_ROOTFS}/etc
    touch ${IMAGE_ROOTFS}/etc/resolv.conf
}
ROOTFS_POSTPROCESS_COMMAND += "rootfs_add_empty_resolv_conf; "

# Custom task to generate the OCI bundle .img from OCI image
do_image_erofs_bundle[depends] += "umoci-native:do_populate_sysroot erofs-utils-native:do_populate_sysroot"
do_image_erofs_bundle[dirs] = "${WORKDIR}"
do_image_erofs_bundle[cleandirs] = "${WORKDIR}/oci-bundle"
fakeroot do_image_erofs_bundle() {
    # Unpack OCI image into a scratch dir
    umoci unpack --rootless --image "${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${OCI_IMAGE_TAG}-oci:${OCI_IMAGE_TAG}" "${WORKDIR}/oci-bundle"

    # move the mtree to deploy dir and do not embed it in the final image
    # (glob must stay unquoted so the shell expands it)
    mv ${WORKDIR}/oci-bundle/*.mtree "${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${OCI_IMAGE_TAG}-oci.mtree"

    # Create the EROFS image from the runtime bundle
    mkfs.erofs -zlz4hc --all-root "${IMGDEPLOYDIR}/${IMAGE_NAME}.img" "${WORKDIR}/oci-bundle"

    # Create a convenient symlink to the image (relative, required by sstate)
    ln -sf "${IMAGE_NAME}.img" "${IMGDEPLOYDIR}/${IMAGE_BASENAME}.img"
}

# Ensure the image gets built after the normal image is done
addtask do_image_erofs_bundle before do_image_complete after do_image_oci