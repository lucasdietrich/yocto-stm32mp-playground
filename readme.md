# overview

- `OK`: means tested and working
- `NOK`: means tested and not working
- `?`: mean not tested yet but interested and should work

| package | version      | mp1 | mp2         | comment                                                                    |
| ------- | ------------ | --- | ----------- | -------------------------------------------------------------------------- |
| tf-a    | st-amy       | OK  |             |                                                                            |
| tf-a    | st           |     | OK          |                                                                            |
| u-boot  | 2026.01      | OK  | ?           |                                                                            |
| u-boot  | 2026.04      | OK  | OK          |                                                                            |
| u-boot  | st 2023.10   |     | ?           |                                                                            |
| optee   | 4.7.0        | OK  |             |                                                                            |
| optee   | 4.8.0        | NOK |             | stm32_rtc_init issue on mp1                                                |
| optee   | 4.9.0        | NOK |             | stm32_rtc_init issue on mp1                                                |
| optee   | 4.10.0       | NOK | NOK (bsec3) | stm32_rtc_init issue on mp1, partial bsec3 support for mp2 (need st patch) |
| optee   | st 4.0.0     | ?   | OK          |                                                                            |
| linux   | amy 6.18     | OK  |             |                                                                            |
| linux   | amy 6.19-rc5 | OK  |             |                                                                            |
| linux   | amy 7.0.10   | OK  | NOK         |                                                                            |
| linux   | st 6.6.116   | ?   | OK          | ST adaptation of linux LTS, next will be 6.18                              |

- note from ST about maintenance of the ST fork of linux: <https://community.st.com/t5/stm32-mpus-embedded-software-and/kernel-version-for-openstlinux-on-stm32mp25-series/td-p/855852>
- notes about linux LTS: <https://www.kernel.org/category/releases.html>

    ```
    Version	Maintainer	Released	Projected EOL
    6.18	Greg Kroah-Hartman & Sasha Levin	2025-11-30	Dec, 2028
    6.12	Greg Kroah-Hartman & Sasha Levin	2024-11-17	Dec, 2028
    6.6	Greg Kroah-Hartman & Sasha Levin	2023-10-29	Dec, 2027
    6.1	Greg Kroah-Hartman & Sasha Levin	2022-12-11	Dec, 2027
    5.15	Greg Kroah-Hartman & Sasha Levin	2021-10-31	Dec, 2026
    5.10	Greg Kroah-Hartman & Sasha Levin	2020-12-13	Dec, 2026
    ```


# stm32mp15 from scratch with yocto (scarthgap)

This repository contains the steps to build a custom BSP layer for the STM32MP15 series.

## Ressource:

1. [Yocto Project Board Support Package Developer’s Guide](https://docs.yoctoproject.org/singleindex.html#document-bsp-guide/index)
2. [STM32CubeMP1](https://github.com/STMicroelectronics/STM32CubeMP1)
    - [STM32MP1 Tips & Tricks - 04 How to debug M4 in production mode with STM32CubeIDE](https://www.youtube.com/watch?v=YIhzzgJmop0)

## Prerequisites

Debian/Ubuntu
```
sudo apt-get install build-essential chrpath cpio debianutils diffstat file gawk gcc git iputils-ping libacl1 libcrypt-dev locales python3 python3-git python3-jinja2 python3-pexpect python3-pip python3-subunit socat texinfo unzip wget xz-utils zstd
```

Arch/CachyOS
```
sudo pacman -Syu --needed base-devel chrpath cpio diffstat file gawk git iputils acl libxcrypt python python-gitpython python-jinja python-pexpect python-pip python-subunit socat texinfo unzip wget xz zstd rpcsvc-proto
```

## Setup build

Source the `.env` file.

Configure the `local.conf`:

```bash
DL_DIR ?= "/home/lucas/yocto/downloads"
SSTATE_DIR ?= "/home/lucas/yocto/sstate-cache"
MACHINE = "dk2"
INHERIT += "rm_work"
RM_WORK_EXCLUDE += ""
AMY_DEBUG = "1"
# BB_NUMBER_THREADS = "16"
PARALLEL_MAKE = "-j16"
```

Configure the `bblayers.conf`:

```bash
BBLAYERS ?= " \
  ${TOPDIR}/../openembedded-core/meta  \
  ${TOPDIR}/../meta-yocto/meta-poky \
  ${TOPDIR}/../meta-yocto/meta-yocto-bsp \
  ${TOPDIR}/../meta-openembedded/meta-oe \
  ${TOPDIR}/../meta-openembedded/meta-python \
  ${TOPDIR}/../meta-openembedded/meta-networking \
  ${TOPDIR}/../meta-openembedded/meta-webserver \
  ${TOPDIR}/../meta-arm/meta-arm-toolchain \
  ${TOPDIR}/../meta-lts-mixins \
  ${TOPDIR}/../meta-swupdate \
  ${TOPDIR}/../meta-bsp-st \
  ${TOPDIR}/../meta-playground \
  "
```

# Yocto (wrynose)

Resources:

- [10 Setting Up the Poky Reference Distro Manually](https://docs.yoctoproject.org/dev-manual/poky-manual-setup.html)
- [Migration notes for 6.0 (wrynose)](https://docs.yoctoproject.org/dev/migration-guides/migration-6.0.html)
    - https://docs.yoctoproject.org/dev/migration-guides/migration-5.1.html
    - https://docs.yoctoproject.org/dev/migration-guides/migration-5.2.html
    - https://docs.yoctoproject.org/dev/migration-guides/migration-5.3.html

local.conf:

```bash
DL_DIR ?= "/home/lucas/yocto/downloads"
SSTATE_DIR ?= "/home/lucas/yocto/sstate-cache"
BB_HASHSERVE_DB_DIR = "${SSTATE_DIR}"
MACHINE = "dk2"
INHERIT += "rm_work"
RM_WORK_EXCLUDE += ""
AMY_DEBUG = "1"
# BB_NUMBER_THREADS = "16"
PARALLEL_MAKE = "-j16"
```

bblayers.conf:

```bash
BBLAYERS ?= " \
  ${TOPDIR}/../openembedded-core/meta  \
  ${TOPDIR}/../meta-yocto/meta-poky \
  ${TOPDIR}/../meta-yocto/meta-yocto-bsp \
  ${TOPDIR}/../meta-openembedded/meta-oe \
  ${TOPDIR}/../meta-openembedded/meta-python \
  ${TOPDIR}/../meta-openembedded/meta-networking \
  ${TOPDIR}/../meta-openembedded/meta-webserver \
  ${TOPDIR}/../meta-arm/meta-arm-toolchain \
  ${TOPDIR}/../meta-lts-mixins \
  ${TOPDIR}/../meta-swupdate \
  ${TOPDIR}/../meta-bsp-st \
  ${TOPDIR}/../meta-playground \
  "
```

Source bitbake:

`source openembedded-core/oe-init-build-env build-wrynose`

# Result

## Expected console output

```
NOTICE:  CPU: STM32MP157FAC Rev.Z
NOTICE:  Model: STMicroelectronics STM32MP157C-DK2 Discovery Board
NOTICE:  Board: MB1272 Var4.0 Rev.C-02
INFO:    PMIC version = 0x20
INFO:    Reset reason (0x15):
INFO:      Power-on Reset (rst_por)
INFO:    FCONF: Reading TB_FW firmware configuration file from: 0x2ffe2000
INFO:    FCONF: Reading firmware configuration information for: stm32mp_io
INFO:    Using SDMMC
INFO:      Instance 1
INFO:    Boot used partition fsbl1
NOTICE:  BL2: v2.10.25(debug):lts-v2.10.25
NOTICE:  BL2: Built : 16:16:09, Oct 10 2025
INFO:    BL2: Doing platform setup
INFO:    RAM: DDR3-DDR3L 16bits 533000kHz
INFO:    Memory size = 0x20000000 (512 MB)
ERROR:   Could NOT find the fip partition!
ERROR:   BL2: Failure in pre image load handling (-2)
```

## TF-A

## OPTEE-OS

hexdump -C out/arm-plat-stm32mp1/core/tee-header_v2.bin
00000000  4f 50 54 45 02 00 00 00  02 00 00 00 00 00 00 00  |OPTE............|
00000010  00 00 fc 2f 00 00 00 00  10 25 01 00 ff ff ff ff  |.../.....%......|
00000020  ff ff ff ff 01 00 00 00  00 50 06 00              |.........P..|
0000002c

```c
struct optee_header_v2 {
        uint32_t magic; // 'OPTE'
        uint8_t version; // 2
        uint8_t arch; // 0 = 32-bit, 1 = 64-bit
        uint16_t flags; // bit 0: debug, bit 1: pager
        uint32_t nb_images; // 2: number of images
        struct optee_image optee_image[]; 
};

struct optee_image {
        uint32_t load_addr_hi; // 0 for 32-bit
        uint32_t load_addr_lo; // load address 0x20000000 for OP-TEE OS and 0x2fc00000 for pager
        uint32_t image_id; // image id: 0 = OP-TEE OS, 1 = pager
        uint32_t size; // size of image in bytes: OP-TEE OS size 0x00012510, pager size 0x00065000
};
```

## Container build

The `Dockerfile` only provides the OS-level build host packages; bitbake,
openembedded-core and the meta-* layers come from this checkout and are
bind-mounted at run time (see `entrypoint.sh`).

Build the image once (rebuild after changing `Dockerfile`/`entrypoint.sh`):

```bash
just yocto-builder
```

Build an image (defaults to `build-wrynose` / `amy-image`):

```bash
just bitbake
just bitbake build_dir=build-mp1-wrynose image=core-image-minimal
```

Or use the wrapper scripts directly, which can also be aliased to shadow the
real tools on your host by sourcing `scripts/aliases.sh` from your shell rc
(works under bash and zsh):

```bash
source scripts/aliases.sh
export DL_DIR="./downloads"
export SSTATE_DIR="./sstate-cache"
export BUILD_DIR="./build-wrynose"
```

```bash
bitbake.sh amy-image -k
devtool.sh status
oe-pkgdata-util.sh list-pkgs
```

`downloads/` and `sstate-cache/` may be plain directories or symlinks to a
shared cache elsewhere on the host; both are handled transparently.