# TODO: patch does not apply

# ERROR: tf-a-st-amy-2.10.24-r0 do_patch: Applying patch '0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch' on target directory '/home/lucas/yocto/yocto-ld-mp1/build/tmp/work/dk2-poky-linux-gnueabi/tf-a-st-amy/2.10.24/git'
# CmdError('quilt --quiltrc /home/lucas/yocto/yocto-ld-mp1/build/tmp/work/dk2-poky-linux-gnueabi/tf-a-st-amy/2.10.24/recipe-sysroot-native/etc/quiltrc push', 0, 'stdout: Applying patch 0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch
# patching file common/bl_common.c
# patching file drivers/amy/io_amy.c
# patching file fdts/stm32mp157f-dk2.dts
# patching file include/drivers/io/io_amy.h
# patching file include/drivers/io/io_storage.h
# patching file include/export/common/tbbr/tbbr_img_def_exp.h
# patching file plat/st/common/bl2_io_storage.c
# Hunk #12 FAILED at 277.
# 1 out of 12 hunks FAILED -- rejects in file plat/st/common/bl2_io_storage.c
# patching file plat/st/common/common.mk
# Hunk #1 succeeded at 206 (offset 10 lines).
# patching file plat/st/common/include/stm32mp_fconf_getter.h
# patching file plat/st/common/include/stm32mp_io_storage.h
# patching file plat/st/common/stm32mp_fconf_io.c
# patching file plat/st/stm32mp1/bl2_plat_setup.c
# Hunk #1 succeeded at 511 (offset 4 lines).
# Hunk #2 succeeded at 559 (offset 4 lines).
# patching file plat/st/stm32mp1/include/platform_def.h
# patching file plat/st/stm32mp1/plat_bl2_mem_params_desc_amy.c
# patching file plat/st/stm32mp1/platform.mk
# Hunk #1 succeeded at 264 (offset 4 lines).
# patching file plat/st/stm32mp1/stm32mp1_def.h
# patching file plat/st/stm32mp1/stm32mp1_fip_def.h
# Patch 0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch does not apply (enforce with -f)

# stderr: ')
# ERROR: Logfile of failure stored in: /home/lucas/yocto/yocto-ld-mp1/build/tmp/work/dk2-poky-linux-gnueabi/tf-a-st-amy/2.10.24/temp/log.do_patch.46479
# ERROR: Task (/home/lucas/yocto/yocto-ld-mp1/meta-bsp-st/recipes-bsp/tf-a/tf-a-st-amy_2.10.24.bb:do_patch) failed with exit code '1'

require tf-a-st_2.10.24.bb

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://0001-Add-support-for-custom-boot-load-raw-OP-TEE-and-u-bo.patch \
    "