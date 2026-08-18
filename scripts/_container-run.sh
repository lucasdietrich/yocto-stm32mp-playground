#!/usr/bin/bash
# Shared podman-run logic for the scripts/*.sh yocto tool wrappers
# (bitbake.sh, devtool.sh, oe-pkgdata-util.sh, ...). Not meant to be run
# directly: source it and call container_run <tool> [args...].
#
# BUILD_DIR selects which build-* directory to use (default: build-wrynose).
# DL_DIR/SSTATE_DIR select which host directory to use for downloads/sstate-cache
# (default: <project_dir>/downloads and <project_dir>/sstate-cache).

container_run() {
    local project_dir downloads_dir sstate_dir
    project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

    # downloads/sstate-cache may be symlinks (e.g. to share caches across
    # projects); resolve them and mount outside /workdir so podman doesn't try
    # to create a mountpoint through the symlink inside the /workdir bind mount.
    downloads_dir="$(readlink -f "${DL_DIR:-${project_dir}/downloads}")"
    sstate_dir="$(readlink -f "${SSTATE_DIR:-${project_dir}/sstate-cache}")"

    podman run --rm -it \
        --userns=keep-id \
        -v "${project_dir}:/workdir:Z" \
        -v "${downloads_dir}:/downloads:Z" \
        -v "${sstate_dir}:/sstate-cache:Z" \
        -e BUILD_DIR="${BUILD_DIR:-build-wrynose}" \
        yocto-builder "$@"
}
