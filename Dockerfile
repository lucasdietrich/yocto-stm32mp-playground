# syntax=docker/dockerfile:1
#
# Yocto Project build container.
# Host package list based on:
# https://docs.yoctoproject.org/brief-yoctoprojectqs/index.html
#
# This image only provides the OS-level toolchain. It does NOT contain
# any Yocto source: bitbake, openembedded-core, and the meta-* layers are
# expected to already exist in your project checkout and are bind-mounted
# in at "podman run" time (see README.md).
#
# Usage:
#   podman build -t yocto-builder -f Dockerfile .
#   podman run --rm -it \
#     --userns=keep-id \
#     -v "$PWD:/workdir:Z" \
#     -v "$PWD/downloads:/workdir/downloads:Z" \
#     -v "$PWD/sstate-cache:/workdir/sstate-cache:Z" \
#     -e BUILD_DIR=build-wrynose \
#     yocto-builder bitbake core-image-sato

FROM ubuntu:24.04

ARG USERNAME=yocto
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Packages listed in the Yocto Project "Build Host Packages" section,
# plus locales/sudo/ca-certificates which the container itself needs.
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        chrpath \
        cpio \
        debianutils \
        diffstat \
        file \
        gawk \
        gcc \
        git \
        iputils-ping \
        libacl1 \
        libcrypt-dev \
        locales \
        python3 \
        python3-git \
        python3-jinja2 \
        python3-pexpect \
        python3-pip \
        python3-subunit \
        socat \
        texinfo \
        tmux \
        unzip \
        wget \
        xz-utils \
        zstd \
        ca-certificates \
        sudo \
    && locale-gen en_US.UTF-8 \
    && update-locale LANG=en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

# Yocto/BitBake refuses to run as root, so create a dedicated user.
# ubuntu:24.04 ships a default "ubuntu" user/group already sitting on
# UID/GID 1000, so remove it first to avoid a clash before creating ours.
RUN if id -u ubuntu >/dev/null 2>&1; then userdel -r ubuntu; fi \
    && if getent group ${USER_GID} >/dev/null; then groupdel "$(getent group ${USER_GID} | cut -d: -f1)"; fi \
    && groupadd --gid ${USER_GID} ${USERNAME} \
    && useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME} \
    && echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER ${USERNAME}

# Mount point for the whole project (bitbake, openembedded-core, meta-*
# layers, and the build-* directories all come from this bind mount).
ENV PROJECT_DIR=/workdir
WORKDIR ${PROJECT_DIR}

# Which build-* directory to initialize (this project has several:
# build, build-mp1-wrynose, build-mp2, build-wrynose, ...).
# Override at run time with: -e BUILD_DIR=build-wrynose
ENV BUILD_DIR=build

COPY --chown=${USERNAME}:${USERNAME} entrypoint.sh /home/${USERNAME}/entrypoint.sh
RUN sudo chmod +x /home/${USERNAME}/entrypoint.sh

ENTRYPOINT ["/home/yocto/entrypoint.sh"]
CMD ["bash"]