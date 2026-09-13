# STM32MP257F-DK — Exposing a UART on CN5 for Linux

## 1. Objective

Expose a UART on the **CN5** GPIO expansion connector (40-pin, Raspberry Pi-compatible header) of the **STM32MP257F-DK**, usable from Linux userspace on the Cortex-A35.

---

## 2. Finding a candidate UART on CN5

Reference: **UM3385** (DK user manual), Table 24 "GPIO connector pinout".

Only **one** USART is explicitly routed on CN5:

| Signal | Pin | Chip pin |
|---|---|---|
| USART6_TX | 8 | PF13 |
| USART6_RX | 10 | PF14 |
| USART6_RTS | 11 | PG5 |
| USART6_CTS | 36 | PF15 |

This matches the standard Raspberry Pi UART position (pins 8/10).

Remaining CN5 pins were checked for hidden alternate-function UARTs via STM32CubeMX:
- Pins on **GPIO port Z** (`PZ0, PZ1, PZ3, PZ4, PZ5, PZ6, PZ7, PZ8, PZ9`) sit in the low-power/isolated domain and do not expose a full USART alternate function.
- Remaining "regular" port pins (`PA1, PA5, PB5, PC4, PC7, PC10, PD0, PD1, PD2, PF0, PF2, PF4, PF7, PF11, PH8`) were checked in CubeMX — none exposed an unused UART TX/RX pair.

**Conclusion: USART6 is the only usable UART on CN5.**

---

## 3. First attempt and failure

Device tree change:

```dts
&usart6 {
    status = "okay";
};
```

Result on target:

```
root@mp2:~# cat /dev/ttySTM1
E/TC:1   stm32_serc_handle_ilac:133 SERC exceptions [63:32]: 0x10
E/TC:1   stm32_serc_handle_ilac:139 SERC exception ID: 36
E/TC:1   Panic at core/drivers/firewall/stm32_serc.c:155 <stm32_serc_handle_ilac>
```

SERC exception **ID 36** corresponds to **USART6** in the RIFSC peripheral ID table.

---

## 4. Root cause: shared USART6 between Cortex-A35 and Cortex-M33

Confirmed by ST support (community forum) for this board family:

> USART6 is used by the UCSI firmware running on the Cortex-M33 (default image `USBPD_DRP_UCSI_CM33_NonSecure_sign.bin`). Accessing it from Linux causes concurrent access (A35 + M33) to the same peripheral and its clock.

Key clarifications reached during the investigation:
- **UCPD1** is the actual dedicated hardware IP for USB-C Power Delivery signaling on STM32MP25 — separate from USART6.
- USART6 is used by the M33 firmware as a **debug/trace UART** for the `USBPD_DRP_UCSI` demonstration, not as the real PD communication bus.
- An initial attempt to disable "trace" at the firmware build-config level appeared to have no effect — see clock evidence in section 6 — which turned out to be caused by a **build system bug**, not by trace being unrelated to USART6 (root cause and fix in section 7).

---

## 5. Understanding the RIF (Resource Isolation Framework) configuration

Board OP-TEE/TF-A security config file: `core/arch/arm/dts/stm32mp257f-dk-ca35tdcid-rif.dtsi`

```c
RIFPROT(STM32MP25_RIFSC_USART6_ID, RIF_UNUSED, RIF_UNLOCK, RIF_NSEC, RIF_NPRIV, RIF_UNUSED, RIF_SEM_DIS, RIF_CFDIS)
```

Macro signature: `RIFPROT(RESOURCE_ID, SEMWL, LOCK, SEC/NSEC, PRIV/NPRIV, CID, SEM_MODE, CID_FILTER_MODE)`

| Field | Value | Meaning |
|---|---|---|
| Resource | `STM32MP25_RIFSC_USART6_ID` | USART6 |
| Semaphore whitelist | `RIF_UNUSED` | No CID whitelisted (irrelevant, semaphore disabled) |
| Lock | `RIF_UNLOCK` | Configuration can still be changed later |
| Security | `RIF_NSEC` | Non-secure world (Linux and/or non-secure M33) may access |
| Privilege | `RIF_NPRIV` | No privileged-mode requirement |
| CID | `RIF_UNUSED` | No enforced owning compartment |
| Semaphore mode | `RIF_SEM_DIS` | No arbitration mechanism active |
| CID filtering | `RIF_CFDIS` | **Disabled** — RIFSC does not check which core is accessing the peripheral |

**Key insight:** RIF is not blocking Linux from USART6 — it left it deliberately *unfiltered*, so both cores can touch it with **no hardware arbitration**. The panic is a genuine concurrent-access clash at a lower level (RCC clock/bus contention), caught by SERC, not a RIF permission denial (contrast with the SPI8/HASH cases where `RIF_SEC` + `RIF_CFEN` do actively block Linux).

---

## 6. Runtime verification methods

### 6.1 Dump live RIFSC configuration (debugfs)

Available via a recent upstream driver patch (`bus: rifsc: add debugfs entry to dump the firewall configuration`, Nov 2025):

```bash
mount -t debugfs none /sys/kernel/debug   # if not already mounted
cat /sys/kernel/debug/stm32_firewall/rifsc
```

Confirmed live state of USART6 matched the static `.dtsi` source:

```
| USART6 || 36 || NSEC || NPRIV || disabled || disabled || 0 || 0x0 |
```

Only security-critical peripherals (`LPTIM1`, `I2C7`, `RNG`, `PKA`, `SAES`, `HASH`, `CRYP2`, `IWDG1/3`, `VREFBUF`, `RAMCFG`, `SERC`) are actually locked (`SEC/PRIV/enabled`) on this board by default; general peripherals including USART6 ship wide open.

### 6.2 Confirm clock ownership outside Linux (proof of concurrent use)

```bash
cat /sys/kernel/debug/clk/clk_summary | grep -i usart6
```

**Before stopping M33 firmware (still running the mis-built firmware, TRACE forced on — see section 7):**
```
ck_icn_p_usart6   0  0  0  200000000  0  0  50000  Y  ...
ck_ker_usart6     0  0  0  64000000   0  0  50000  Y  ...
```

**After manually stopping M33 firmware (`echo stop > /sys/class/remoteproc/remoteproc1/state`):**
```
ck_icn_p_usart6   0  0  0  200000000  0  0  50000  N  ...
ck_ker_usart6     0  0  0  64000000   0  0  50000  N  ...
```

Linux's own `enable_cnt`/`prepare_cnt` stayed at `0 0 0` throughout — Linux never requested these clocks. The hardware-level `Y → N` flag change proved the **M33 firmware was enabling these clocks directly at init**, independent of Linux.

At this point in the investigation this was read as "trace setting doesn't matter, only stopping the firmware helps" — but the real explanation was that the firmware's trace option was never actually being honored by the build. See section 7 for the actual root cause and fix, which resolved this without needing to manually stop the M33 firmware.

---

## 7. Final root cause and fix: `_TRACE` macro was hardcoded on in the DK build

The clock evidence in section 6 showed the M33 firmware was actively driving USART6 regardless of the `TRACE` build option being set to `OFF`. This turned out to be a **build system bug** in the STM32CubeMP2 delivery, not a hardware/firmware design fact — the `TRACE` option really does control whether the firmware touches USART6, it just wasn't being applied.

### Investigation

Build performed with:
```bash
source /opt/amy/1.0/environment-setup-cortexa35-poky-linux
cmake --fresh -B build -S . -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
make -C build
```

`Projects/STM32MP257F-DK/Demonstrations/USBPD_DRP_UCSI/CMakeLists.txt` contained the expected guard:
```cmake
option(TRACE "USBPD Trace feature (_TRACE macro)" OFF)
if(TRACE)
    add_definitions(-D_TRACE)
endif()
```
...but the `_TRACE` macro was still active in the resulting build regardless of this setting.

### Root cause

The **same** `CMakeLists.txt` also had `-D_TRACE` **hardcoded directly into the `DEFINITIONS` string**, unconditionally, entirely separate from the `option(TRACE)` / `if(TRACE)` guard above. This hardcoded definition silently overrode the option — `-DTRACE=ON`/`OFF` at configure time had no effect.

By comparison, the **EV1** variant (`Projects/STM32MP257F-EV1/Demonstrations/USBPD_DRP_UCSI/CMakeLists.txt`) did not have this hardcoded define, and correctly respected the `TRACE` option — which is why this class of problem is specific to the **DK** board's project files.

### Fix applied

- Removed the hardcoded `-D_TRACE` entry from the `DEFINITIONS` string in the DK `CMakeLists.txt`.
- Added the same `option(TRACE OFF)` / `if(TRACE) add_definitions(-D_TRACE) endif()` block already used in the EV1 `CMakeLists.txt`, so `_TRACE` now defaults `OFF` and can be explicitly re-enabled with:
  ```bash
  cmake -DTRACE=ON -B build -S .
  ```

**Follow-up note:** an existing `build/` directory configured before this change will not pick it up with a plain `make` — it must be reconfigured:
```bash
cmake --fresh -B build -S .
make -C build
```

**Commit message used:**
```
build(cmake): make _TRACE define opt-in via TRACE option in DK USBPD_DRP_UCSI

Remove hardcoded -D_TRACE from DEFINITIONS and gate it behind an
option(TRACE OFF), matching the EV1 demo's CMakeLists.txt.
```

### Outcome

With the corrected build actually compiled with `_TRACE` off, the M33 firmware no longer initializes/clocks USART6 at boot — resolving the conflict with Linux **without needing to manually stop the M33 remoteproc firmware** on every boot, and without disabling USB-C PD/UCSI functionality.

### Remaining caution

The manual `echo stop > .../state` workaround used earlier in the investigation (section 6) was a useful **diagnostic** step to prove clock ownership, but is no longer needed as the fix. If reverting to a stock/unmodified M33 firmware image in the future, note that a prior report showed manually stopping this class of default firmware caused instability/reboot on an EV1 board — the build fix above is the safer, permanent solution.

---

## 8. UART validation procedure (TX↔RX loopback)

Run after rebuilding/reflashing the corrected M33 firmware, to confirm USART6 is now Linux-exclusive and functional.

With TX physically wired to RX on the same USART instance:

```bash
# 1. Confirm device node + no SERC errors
ls -l /dev/ttySTM1
dmesg | grep -i usart

# 2. Configure raw mode
stty -F /dev/ttySTM1 115200 cs8 -cstopb -parenb raw -echo

# 3. Simple two-terminal test
# Terminal A:
cat /dev/ttySTM1
# Terminal B:
echo "hello loopback" > /dev/ttySTM1
```

Common gotchas: flow control (RTS/CTS) requiring `-crtscts` if those pins aren't wired; pinctrl "idle" state putting pins in analog mode between transfers; `dialout`/`tty` group permissions.

---

## 9. Summary timeline

1. Located GPIO AF tables → datasheet + STM32CubeMX.
2. Identified CN5 as the RPi-compatible 40-pin header; only USART6 is wired for UART use.
3. Enabling USART6 in Linux DT caused a SERC panic (exception ID 36).
4. Identified USART6 is shared with the default M33 `USBPD_DRP_UCSI` firmware; RIF leaves it unfiltered (`RIF_CFDIS`), so no hardware arbitration prevents concurrent access.
5. Verified via `debugfs` RIFSC dump and `clk_summary` that the M33 firmware — not RIF — was gating the USART6 clock.
6. An initial attempt to disable the firmware's `TRACE` build option appeared to have no effect on this clock behavior.
7. **Root cause found:** the DK's `USBPD_DRP_UCSI` `CMakeLists.txt` hardcoded `-D_TRACE` unconditionally, bypassing the `option(TRACE OFF)` guard — so trace was never actually disabled by prior attempts. Fixed by removing the hardcoded define and adopting the same conditional block used in the EV1 project's `CMakeLists.txt`.
8. After rebuilding with a clean `cmake --fresh` reconfigure and reflashing, the M33 firmware no longer clocks/claims USART6 — Linux can use it exclusively, with no need to manually stop the M33 firmware and without losing USB-C PD/UCSI functionality.
9. Validated the UART with a TX/RX hardware loopback test suite.