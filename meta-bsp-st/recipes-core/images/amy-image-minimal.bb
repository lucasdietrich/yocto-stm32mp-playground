SUMMARY = "stm32mp Amy Image"
LICENSE = "MIT"

IMAGE_INSTALL = "\
    packagegroup-core-boot \
    packagegroup-amy-bsp \
    ${CORE_IMAGE_EXTRA_INSTALL} \
"

# install all the kernel modules
IMAGE_INSTALL += "kernel-modules"

inherit core-image

IMAGE_ROOTFS_SIZE ?= "8192"

TOOLCHAIN_HOST_TASK += "\
    nativesdk-genimage \
    nativesdk-gcc-arm-none-eabi \
    nativesdk-dtc \
    "

EXTRA_IMAGEDEPENDS += "virtual/tf-a"

IMAGE_FSTYPES = "ext4"
IMAGE_FSTYPES += "ext4.zst"

#########################
# SWUpdate image        #
#########################

python() {
  d.setVarFlag("SWUPDATE_IMAGES_FSTYPES", d.getVar("IMAGE_BASENAME"), ".ext4.zst")
}

# do not include all SRC_URI files in the swupdate image
SWUPDATE_SRC_URI_EXCLUDE += "${SDIMAGE_CONF}"

# swupdate image
inherit swupdate-image

SRC_URI += "\
    file://sw-description \
    file://update-script.sh \
"

#########################
# Create a sdcard image #
#########################
# This is required because of https://stackoverflow.com/a/55570545
python () {
    # Ensure we run these usually noexec tasks
    d.delVarFlag("do_fetch", "noexec")
    d.delVarFlag("do_unpack", "noexec")
}

SRC_URI += "file://${SDIMAGE_CONF}"

DEPENDS += "dosfstools-native mtools-native genimage-native"
DEPENDS += "virtual/tf-a"
DEPENDS += "u-boot-scr"
DEPENDS:append:mp1 = " virtual/optee-os "
DEPENDS:append:mp1 = " virtual/u-boot "
DEPENDS:append:mp1 = " virtual/kernel "
DEPENDS:append:mp2 = "fip"

TOOLCHAIN_HOST_TASK += "\
    nativesdk-tf-a-tools \
"

SDIMAGE_DEPENDS := "\
    virtual/tf-a:do_deploy \
    virtual/u-boot:do_deploy \
    u-boot-scr:do_deploy \
    virtual/optee-os:do_deploy \
    virtual/kernel:do_deploy \
    "
SDIMAGE_DEPENDS:append:mp2 = "fip:do_deploy"

do_sdimage[depends] += "${SDIMAGE_DEPENDS}"
addtask do_sdimage after do_image_ext4 before do_image_complete

do_sdimage() {
    bbnote "Generating sdcard image"

    # clean up existing .img files
    rm -f ${IMGDEPLOYDIR}/*.img ${IMGDEPLOYDIR}/*.img.gz
    
    sed -e "s|@IMAGE@|${IMAGE_BASENAME}|g" \
        -e "s|@MACHINE@|${MACHINE}|g" \
        -e "s|@TFA_DEVICETREE@|${TFA_DEVICETREE}|g" \
        -e "s|@IMAGE_ROOTFS@|${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.ext4|g" \
        -e "s|@KERNEL_DEVICETREE_NAME@|${KERNEL_DEVICETREE_NAME}|g" \
        ${FILE_DIRNAME}/files/${SDIMAGE_CONF} > ${WORKDIR}/sdcard_genimage-${IMAGE_BASENAME}-${MACHINE}.cfg

    mkdir -p ${WORKDIR}/genimage/tmp ${WORKDIR}/genimage/root

    genimage \
        --config ${WORKDIR}/sdcard_genimage-${IMAGE_BASENAME}-${MACHINE}.cfg \
        --root ${WORKDIR}/genimage/root \
        --tmppath ${WORKDIR}/genimage/tmp \
        --inputpath ${DEPLOY_DIR_IMAGE} \
        --outputpath ${IMGDEPLOYDIR}

    gzip -k -9 ${IMGDEPLOYDIR}/*.img

    cp ${WORKDIR}/sdcard_genimage-${IMAGE_BASENAME}-${MACHINE}.cfg ${IMGDEPLOYDIR}
}

IMAGE_OVERHEAD_FACTOR = "1.0"
IMAGE_ROOTFS_EXTRA_SPACE = "32768"
IMAGE_ROOTFS_MAXSIZE = "524288"

# userfs directory
rootfs_prepare_userfs() {
    install -d ${IMAGE_ROOTFS}/mnt/userfs
}
ROOTFS_POSTPROCESS_COMMAND += "rootfs_prepare_userfs;"