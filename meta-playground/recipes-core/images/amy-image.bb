require recipes-core/images/amy-image-minimal.bb

IMAGE_INSTALL:append = "\
    packagegroup-core-ssh-dropbear \
    packagegroup-core-base-utils \
    htop \
"

IMAGE_INSTALL:append = " \
    packagegroup-amy-examples \
"

IMAGE_INSTALL:append = "${@bb.utils.contains('AMY_DEBUG_UTILS', '1',' \
    util-linux \
    e2fsprogs-resize2fs \
    e2fsprogs-e2fsck \
    e2fsprogs \
    btrfs-tools \
    gdbserver \
    htop \
    iotop \
    lsof \
    strace \
    socat \
    valgrind \
    iperf3 \
    smemstat \
    tcpdump \
    perf \
    curl \
    nano \
    tree \
    canutils \
    can-utils \
    dtc \
    git \
    bash-completion \
    minicom \
    mosquitto-clients \
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