
## Instructions to build the TF-A for STM32MP1

```
 . /opt/amy/1.0/environment-setup-cortexa7t2hf-neon-vfpv4-poky-linux-gnueabi && \
     unset LDFLAGS && unset CFLAGS && unset CPPFLAGS && \
     make CROSS_COMPILE=arm-none-eabi- \
     STM32MP_SDMMC=1 \
     PLAT=stm32mp1 \
     ARCH=aarch32 ARM_ARCH_MAJOR=7 \
     DTB_FILE_NAME=stm32mp157c-dk2.dtb \
     LOG_LEVEL=40 \
     -j16
```

## Instructions to build the TF-A for STM32MP2

```
unset LDFLAGS && unset CFLAGS && unset CPPFLAGS && \
    bear -- make CROSS_COMPILE=aarch64-none-elf- \
    STM32MP_SDMMC=1 \
    PLAT=stm32mp2 \
    ARCH=aarch64 ARM_ARCH_MAJOR=8 \
    DTB_FILE_NAME=stm32mp257f-dk.dtb \
    STM32MP_LPDDR4_TYPE=1 \
    LOG_LEVEL=40 \
    DEBUG=1 \
    -j16
```