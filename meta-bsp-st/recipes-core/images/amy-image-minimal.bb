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
    nativesdk-tf-a-tools \
"

EXTRA_IMAGEDEPENDS += "virtual/tf-a"

IMAGE_FSTYPES = "ext4"
IMAGE_FSTYPES += "ext4.zst"

# sysctl
SRC_URI += "file://sysctl.conf"

# swupdate-common.bbclass (pulled in below via "inherit swupdate-image") sets
# S = "${UNPACKDIR}", and image.bbclass's do_rootfs[cleandirs] wipes ${S}
# before ROOTFS_POSTPROCESS_COMMAND runs, so keep a copy of ${UNPACKDIR} files outside of it.
do_unpack[postfuncs] += "copy_src_files"
copy_src_files() {
    cp ${UNPACKDIR}/* ${WORKDIR}
}

sysctl() {
    install -m 0442 ${WORKDIR}/sysctl.conf ${IMAGE_ROOTFS}${sysconfdir}/sysctl.conf
}
ROOTFS_POSTPROCESS_COMMAND += "sysctl;"

#########################
# SWUpdate image        #
#########################

python() {
  d.setVarFlag("SWUPDATE_IMAGES_FSTYPES", d.getVar("IMAGE_BASENAME"), ".ext4.zst")
}

SDIMAGE_CONF ??= "sdcard_genimage.cfg.in"

# do not include all SRC_URI files in the swupdate image
SWUPDATE_SRC_URI_EXCLUDE += "${SDIMAGE_CONF} sysctl.conf"

# include bootloader artifacts in the SWU for MP2 (A-slot only)
SWUPDATE_IMAGES:append = " tf-a/tf-a-${TFA_DEVICETREE}.stm32"
SWUPDATE_IMAGES:append = " ${IMAGE_BASENAME}-${MACHINE}-bootfs.vfat.zst"
SWUPDATE_IMAGES:append:mp2 = " fip/fip-${MACHINE}.bin"
SWUPDATE_IMAGES:append:mp1 = " u-boot/u-boot.bin"
SWUPDATE_IMAGES:append:mp1 = " optee-os/tee-header_v2.bin optee-os/tee-pager_v2.bin optee-os/tee-pageable_v2.bin"

# swupdate image
inherit swupdate-image
SRC_URI += "\
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
        ${WORKDIR}/${SDIMAGE_CONF} > ${WORKDIR}/sdcard_genimage-${IMAGE_BASENAME}-${MACHINE}.cfg

    mkdir -p ${WORKDIR}/genimage/tmp ${WORKDIR}/genimage/root

    genimage \
        --config ${WORKDIR}/sdcard_genimage-${IMAGE_BASENAME}-${MACHINE}.cfg \
        --root ${WORKDIR}/genimage/root \
        --tmppath ${WORKDIR}/genimage/tmp \
        --inputpath ${DEPLOY_DIR_IMAGE} \
        --outputpath ${IMGDEPLOYDIR}

    gzip -k -9 ${IMGDEPLOYDIR}/*.img

    zstd -T0 -19 -f ${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}-bootfs.vfat \
        -o ${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}-bootfs.vfat.zst

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