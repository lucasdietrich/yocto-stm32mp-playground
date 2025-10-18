SUMMARY = "stm32mp1 Amy Image"
LICENSE = "MIT"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

inherit core-image

IMAGE_ROOTFS_SIZE ?= "8192"

TOOLCHAIN_HOST_TASK += "\
    nativesdk-genimage \
    nativesdk-gcc-arm-none-eabi \
    nativesdk-dtc \
    "

EXTRA_IMAGEDEPENDS += "virtual/tf-a"

#########################
# Create a sdcard image #
#########################
# This is required because of https://stackoverflow.com/a/55570545
python () {
    # Ensure we run these usually noexec tasks
    d.delVarFlag("do_fetch", "noexec")
    d.delVarFlag("do_unpack", "noexec")
}

SRC_URI += "file://sdcard_genimage.cfg.in"

DEPENDS += "genimage-native"

do_sdimage[depends] += "virtual/tf-a:do_deploy"
addtask do_sdimage after do_image_ext4 before do_image_complete

do_sdimage() {
    bbnote "Generating sdcard image"

    sed -e "s|@IMAGE@|${IMAGE_BASENAME}|g" \
        -e "s|@MACHINE@|${MACHINE}|g" \
        -e "s|@TFA_DEVICETREE@|${TFA_DEVICETREE}|g" \
        ${WORKDIR}/sdcard_genimage.cfg.in > ${WORKDIR}/sdcard_genimage.cfg

    mkdir -p ${WORKDIR}/genimage/tmp ${WORKDIR}/genimage/root ${WORKDIR}/genimage/input
    ln -sf ${DEPLOY_DIR_IMAGE}/tf-a/* ${WORKDIR}/genimage/input/

    # clean up existing .img files
    rm -f ${IMGDEPLOYDIR}/*.img ${IMGDEPLOYDIR}/*.img.gz

    genimage \
        --config ${WORKDIR}/sdcard_genimage.cfg \
        --root ${WORKDIR}/genimage/root \
        --tmppath ${WORKDIR}/genimage/tmp \
        --inputpath ${WORKDIR}/genimage/input \
        --outputpath ${IMGDEPLOYDIR}

    gzip -k -9 ${IMGDEPLOYDIR}/*.img

    cp ${WORKDIR}/sdcard_genimage.cfg ${IMGDEPLOYDIR}
}