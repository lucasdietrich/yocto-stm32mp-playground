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
MACHINE = "dk2"
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