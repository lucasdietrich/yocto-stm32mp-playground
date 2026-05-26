require optee-os.inc

COMPATIBLE_MACHINE = "(mp2)"

SRCREV = "2a5b1d1232f582056184367fb58a425ac7478ec6"

SRC_URI += " \
    https://raw.githubusercontent.com/STMicroelectronics/meta-st-stm32mp/bcadba4d92cbfeba7b7a876c2e828f37a70d0d0a/recipes-security/optee/optee-os-stm32mp/0001-4.0.0-stm32mp-r3.patch\
"
SRC_URI[sha256sum] = "c3ccf9f87aae9e8763b4c04f0e8fe835a15548c890b9c570d8d5abd3dcddda89"

EXTRA_OEMAKE += "CFG_WITH_TUI=n"

# scp-firmware 2.13 is already embedded in the r3 patch from ST
PACKAGECONFIG:remove = "scpfw"