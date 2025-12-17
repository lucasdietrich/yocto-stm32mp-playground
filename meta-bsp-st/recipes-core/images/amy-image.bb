require amy-image-minimal.bb

IMAGE_ROOTFS_SIZE = "16384"

IMAGE_INSTALL:append = "\
    packagegroup-core-ssh-dropbear \
    htop \
"