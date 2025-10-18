# stm32mp15 from scratch with yocto

This repository contains the steps to build a custom BSP layer for the STM32MP15 series.

## Ressource:

1. [Yocto Project Board Support Package Developer’s Guide](https://docs.yoctoproject.org/singleindex.html#document-bsp-guide/index)
2. [STM32CubeMP1](https://github.com/STMicroelectronics/STM32CubeMP1)
    - [STM32MP1 Tips & Tricks - 04 How to debug M4 in production mode with STM32CubeIDE](https://www.youtube.com/watch?v=YIhzzgJmop0)

## Setup build

Source the `.env` file.

Configure the `local.conf`:

```bash
DL_DIR ?= "/home/lucas/yocto/downloads"
SSTATE_DIR ?= "/home/lucas/yocto/sstate-cache"
INHERIT += "rm_work"
RM_WORK_EXCLUDE += ""
```

Configure the `bblayers.conf`:

```bash
BBLAYERS ?= " \
  /home/lucas/yocto/yocto-ld-mp1/poky/meta \
  /home/lucas/yocto/yocto-ld-mp1/poky/meta-poky \
  /home/lucas/yocto/yocto-ld-mp1/poky/meta-yocto-bsp \
  /home/lucas/yocto/yocto-ld-mp1/meta-bsp-st \
  /home/lucas/yocto/yocto-ld-mp1/meta-openembedded/meta-oe \
  /home/lucas/yocto/yocto-ld-mp1/meta-openembedded/meta-python \
  /home/lucas/yocto/yocto-ld-mp1/meta-openembedded/meta-networking \
  /home/lucas/yocto/yocto-ld-mp1/meta-arm/meta-arm-toolchain \
  "
```