# Fall back to meta-bsp-st for shared files (update-script.sh)
#
# is there an alternative for this trick ?
# the other solution is to copy all the files 
# from meta-bsp-st/recipes-core/images/files 
# to meta-playground/recipes-core/images/files
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../meta-bsp-st/recipes-core/images/files:"

require recipes-core/images/amy-image-minimal.bb

IMAGE_INSTALL:append = "\
    packagegroup-core-ssh-dropbear \
    packagegroup-core-base-utils \
    htop \
"

IMAGE_INSTALL:append = " \
    packagegroup-amy-core \
    packagegroup-amy-examples \
"

# utils
IMAGE_INSTALL:append = "${@bb.utils.contains('AMY_DEBUG_UTILS', '1',' \
    util-linux \
    e2fsprogs-resize2fs \
    e2fsprogs-e2fsck \
    erofs-utils \
    e2fsprogs \
    btrfs-tools \
', '', d)}"

# debug / benchmark
IMAGE_INSTALL:append = "${@bb.utils.contains('AMY_DEBUG_UTILS', '1',' \
    gdbserver \
    htop \
    iotop \
    lsof \
    strace \
    socat \
    valgrind \
    smemstat \
    tcpdump \
    usbutils \
    iperf3 \
    perf \
', '', d)}"

# benchmark
IMAGE_INSTALL:append = "${@bb.utils.contains('AMY_DEBUG_UTILS', '1',' \
    canutils \
    can-utils \
', '', d)}"

# features
IMAGE_INSTALL:append = "${@bb.utils.contains('AMY_DEBUG_UTILS', '1',' \
    curl \
    nano \
    tree \
    git \
    bash-completion \
    minicom \
    mosquitto-clients \
    dtc \
', '', d)}"

EXTRA_IMAGE_FEATURES = "\
    allow-root-login \
"

EXTRA_IMAGE_FEATURES += "${@bb.utils.contains('AMY_DEBUG_SSH', '1',' \
    debug-tweaks \
    empty-root-password \
    allow-empty-password \
    post-install-logging \
', '', d)}"

TOOLCHAIN_HOST_TASK += "packagegroup-rust-cross-canadian-${MACHINE} \
                        nativesdk-erofs-utils \
                        nativesdk-systemtap \
                        "