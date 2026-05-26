SUMMARY = "Machine-specific U-Boot boot script"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "^(mp1|mp2)$"

DEPENDS = "u-boot-mkimage-native"
INHIBIT_DEFAULT_DEPS = "1"

PROVIDES += "u-boot-default-script"

SRC_URI = " \
    file://boot.cmd.in \
"

UBOOTSCR_KERNEL_IMAGE ??= "${KERNEL_IMAGETYPE}"
UBOOTSCR_DTB ??= "${@((d.getVar('KERNEL_DEVICETREE') or '').split()[0].split('/')[-1]) if (d.getVar('KERNEL_DEVICETREE') or '').strip() else ''}"
UBOOTSCR_BOOT_CMD ??= "${@'bootz' if 'zImage' in (d.getVar('KERNEL_IMAGETYPE') or '').split() else ('bootm' if 'uImage' in (d.getVar('KERNEL_IMAGETYPE') or '').split() else 'booti')}"

inherit kernel-arch deploy nopackages

do_compile() {
    sed -e 's|@@MACHINE@@|${MACHINE}|' \
        -e 's|@@KERNEL_IMAGE@@|${UBOOTSCR_KERNEL_IMAGE}|' \
        -e 's|@@DTB_FILE@@|${UBOOTSCR_DTB}|' \
        -e 's|@@BOOT_CMD@@|${UBOOTSCR_BOOT_CMD}|' \
        ${WORKDIR}/boot.cmd.in > ${WORKDIR}/boot.cmd

    mkimage -A ${UBOOT_ARCH} -T script -C none -n "Amy ${MACHINE} boot script" \
        -d ${WORKDIR}/boot.cmd ${WORKDIR}/boot.scr
}

do_deploy() {
    install -d ${DEPLOYDIR}/u-boot-scr
    install -m 0644 ${WORKDIR}/boot.cmd ${DEPLOYDIR}/u-boot-scr/boot.cmd
    install -m 0644 ${WORKDIR}/boot.scr ${DEPLOYDIR}/u-boot-scr/boot.scr
}

addtask do_deploy after do_compile before do_build
