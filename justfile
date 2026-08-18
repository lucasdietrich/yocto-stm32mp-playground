flash:
    ./scripts/flash.sh

yocto-builder:
    podman build -t yocto-builder -f Dockerfile .

bitbake build_dir="build-wrynose" image="amy-image":
    podman run --rm -it \
    --userns=keep-id \
    -v "$PWD:/workdir:Z" \
    -v "$(readlink -f $PWD/downloads):/downloads:Z" \
    -v "$(readlink -f $PWD/sstate-cache):/sstate-cache:Z" \
    -e BUILD_DIR={{build_dir}} \
    yocto-builder bitbake {{image}} -k