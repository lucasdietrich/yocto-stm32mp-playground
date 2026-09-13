# STM32MP257F-DK Cortex-M0+ Zephyr Port — Synthesis

Status as of 2026-09-13: **hardware-validated** on a real STM32MP257F-DK
board — `hello_world`-style periodic console print (LPUART1) + LED blink
(GPIOZ) run together on the M0+ coprocessor and were observed working over
`/dev/ttySTM1` on the Linux (Cortex-A35) side.

This document summarizes what was done, why, and what is left. It is meant
to be read together with:
- `patches/0001-zephyr-stm32mp257f-dk-cortex-m0plus-port.patch` — all
  changes to the `zephyr` repo.
- `patches/0002-hal_stm32-stm32mp257f-dk-cortex-m0plus-port.patch` — all
  changes to the `modules/hal/stm32` (hal_stm32) repo.
- `GETTING_STARTED.md` — how to apply the patches and build/flash the
  two sample applications.
- `OPEN_POINTS.md` — unresolved questions and remaining work.

## Why this is hard

The STM32MP257 is a heterogeneous multi-core SoC: Cortex-A35 (runs Linux),
Cortex-M33 (real-time secure/non-secure coprocessor), and Cortex-M0+ (an
ultra-low-power coprocessor called "CPU3" in ST's docs). Zephyr already had
an M33 port for this board/SoC family; **no M0+ (CPU3) port existed
anywhere in Zephyr, upstream or otherwise** — this work created one from
scratch.

The M0+ core is architecturally crippled by design (that's the point — it's
meant for ultra-low-power tasks while A35/M33 sleep):
- It can only execute code from a **32 KB LPSRAM region**
  (`0x200C0000`–`0x200C7FFF`) — no access to DDR or the main flash.
- It can only access a small, fixed subset of peripherals, ST's
  "SmartRun domain": GPIOZ, LPUART1, HSEM, IPCC2, PWR, and a couple of
  EXTI lines — confirmed both from the reduced CMSIS header
  (`stm32mp257fxx_cm0.h`) and from ST's own bare-metal CM0PLUS demo (which
  only links `stm32mp2xx_hal_{cortex,gpio,hsem,ipcc,pwr,pwr_ex,uart,uart_ex}.c`).
- It has **no access to RCC (clock controller)**: all peripheral clock
  gating for peripherals assigned to M0+ must be done by the Cortex-A35
  (Linux) resource manager *before* the M0+ firmware starts, via
  `remoteproc`. Attempting to touch RCC registers from M0+ triggers an
  IAC/RIF bus fault.
- It has no FPU, no DWT, and only 2 combined NVIC lines for all 16 GPIOZ
  EXTI sources (vs. 16 separate lines on M33).

Because Zephyr's shared STM32 HAL/driver layer (`modules/hal/stm32`,
`zephyr/soc/st/stm32/common/`, `zephyr/drivers/*/`.../`*_stm32.c`) was
written assuming "a normal STM32 core with full RCC/peripheral access,"
almost every subsystem needed a small, targeted fix to work on this
reduced core. None of the fixes touch M33/A35 behavior.

## Phases

### Phase 0 — Research
Read ST's bare-metal `CM0PLUS_DEMO` reference project and internal
debug notes to establish: boot/reset model (M0+ is always a secondary
core, held in reset until A35/M33 releases it via `remoteproc`), the
LPSRAM memory map, the "no RCC access" constraint, and the peripheral
surface actually used by ST's own demo.

### Phase 1 — `hello_world` boots
Got a minimal Zephyr image (`zephyr/samples/hello_world`) to build, link,
and boot on M0+ within the 16 KB/8 KB LPSRAM code/data budget. This
required 13 separate fixes across the shared STM32 HAL/SoC layer — see
the numbered checklist in the patch and in
`/memories/repo/stm32mp257-m0plus-port.md` for full detail. Highlights:
- Added the M0+ SoC/board scaffolding (`Kconfig`, `soc.yml`, `m0plus/`
  SoC subdir, board `.dts`/`.yaml`/`_defconfig`), mirroring the existing
  M33 port.
- Gated ~28 HAL module `#define`s in the shared, non-per-core
  `stm32mp2xx_hal_conf.h` behind `#if !defined(CORE_CM0PLUS)` (ADC, RCC,
  TIM, CRC, DCACHE, ETH, ... — anything M0+'s CMSIS header doesn't define
  register types for).
- Added a `-DCORE_CM0PLUS` compile define and wired
  `system_stm32mp2xx_m0plus.c` (a vendor file that already existed but was
  never referenced by any CMakeLists.txt) into the build, replacing the
  previously-unconditional M33 file, and no-op-stubbed out
  `stm32mp2xx_hal_rcc.c`/`_rcc_ex.c` from the M0+ build.
- Fixed `zephyr/soc/st/stm32/common/{soc_config.c,stm32_backup_domain.c}`
  (DBGMCU / backup-domain access, both unavailable on M0+) and
  `gpioport_mgr.c` (unconditional `DEVICE_DT_GET(DT_NODELABEL(rcc))` broke
  any SoC without an `rcc` DT node — fixed generically, safe for all SoCs).
- Added a devicetree `exti2` interrupt-controller node matching M0+'s 2
  combined NVIC lines (not M33's 16), and fixed **two real upstream Zephyr
  bugs** in `intc_exti_stm32_priv.h`/`intc_gpio_stm32.c` along the way
  (`CPU_CORTEX_M0` vs `CPU_CORTEX_M0PLUS` Kconfig confusion; wrong
  `LL_EXTI_SetEXTISource()` arity assumption for MP2's 3-arg signature).
- `CONFIG_CPU_CORTEX_M_HAS_SYSTICK` had to be selected explicitly for
  M0+ (missing → no system timer, `sys_clock_elapsed` link failure).

Result: `west build -b stm32mp257f_dk/stm32mp257fxx/m0plus
samples/hello_world` → FLASH 9736 B/16 KB, RAM 2016 B/8 KB. Not yet
hardware-tested at this point (console wasn't wired up yet).

### Phase 2a — Blinky (GPIOZ)
Added a `gpio-leds`/`led0` alias in the board `.dts` for PZ0 (GPIOZ pin 0
— not a real board LED, just the only GPIO bank reachable from M0+, used
as a generic test pin). `samples/basic/blinky` built and ran **with zero
additional code changes** — the Phase 1 `gpioport_mgr.c` no-rcc-node stub
already covered GPIO's clock-enable path.
**Hardware-validated**: LED confirmed blinking on PZ0.

### Phase 2b — LPUART1 console
This was the hardest phase. `uart_stm32.c` (the shared STM32 UART driver)
assumes a real, working `clock_control` device at `DT_NODELABEL(rcc)` and
a real `reset` controller — neither of which M0+ can have. Rather than
patch the shared, generic clock_control/reset drivers used by every other
STM32 chip (high blast radius), two **new, M0+-only no-op stub drivers**
were added under `zephyr/soc/st/stm32/stm32mp2x/m0plus/`:
- `rcc_stub.c` — implements the `clock_control` API compatible with a new
  DT compatible `st,stm32mp2-rcc-m0stub` (on/off/get_status are no-ops
  that report "already on"; `get_rate()` returns the clock frequency —
  see the Phase 2c fix below for why this matters).
- `reset_stub.c` — implements the `reset` API for a new compatible
  `st,stm32-rcc-rctl-m0stub` (all operations are no-ops).

New DT nodes (`rcc`, `rctl`, `lpuart1`) were added to
`stm32mp2_m0plus.dtsi` using real LPUART1 register offsets from
`stm32mp257fxx_cm0.h` (base `0x46030000`, IRQ 11) and the RCC/reset macro
offsets discovered in the MP2 combined CFGR register layout
(`RCC_LPUART1CFGR = 0x7A0`, EN bit 1, RST bit 0).

A second, unrelated snag: ST's generated pinctrl file
(`stm32mp257faix-pinctrl.dtsi`) unconditionally does `&pinctrl { ... }`,
which requires a node *literally labeled* `pinctrl` to exist — M0+'s dtsi
only had a bare `gpioz` node. Fixed by wrapping `gpioz` inside a new
`pinctrl: pin-controller@46200000 { ... }` parent (scoped to just the
GPIOZ register range, not the full pin-controller block M33 uses).

LPUART1 was wired to **TX=PZ7, RX=PZ8** (chosen to avoid conflicting with
the PZ0 blinky pin; PZ0/PZ9 and PZ1/PZ4 were the other available options).

**The actual root-cause compile bug**: `uart_stm32.c` includes
`stm32_ll_usart.h`, whose entire content — including the *generic*
inline register-access helpers also used for LPUART — is guarded by
`#if defined(USART1) || ... || defined(UART9)`. M0+'s CMSIS header
defines only `LPUART1`, no `USART1..UART9` macro at all (those
peripherals genuinely aren't in the M0+ core's memory map), so the guard
was always false and every `LL_USART_*` symbol vanished. **Fix**: a
one-line `zephyr_compile_definitions(USART1=LPUART1)` in the M0+
`CMakeLists.txt` — safe because the guard only checks `defined()`, never
expands the macro, and nothing else references a bare `USART1` identifier
in M0+ code.

Result: `west build -b stm32mp257f_dk/stm32mp257fxx/m0plus
samples/hello_world` (LPUART1 console) → FLASH 13036 B/16 KB (79.6%),
RAM 2 KB/8 KB (25%). Build-only at this point.

### Phase 2c — Hardware bring-up bug: wrong LPUART1 clock rate
First hardware test produced a single repeating garbage byte
(`xxxxxxx...`) on `/dev/ttySTM1` instead of text — the classic signature
of a UART baud-rate mismatch, not a wiring problem. Root cause:
`rcc_stub.c`'s `get_rate()` returned `CONFIG_SYS_CLOCK_HW_CYCLES_PER_SEC`
(64 MHz, the M0+ *core* clock) for every clock query, but `uart_stm32.c`
uses that value as LPUART1's *kernel* clock (`ck_ker_lpuart1`), which is
an independently-configured RCC clock-mux output — **not** derived from
the core clock. The real value, read from the Linux side via
`cat /sys/kernel/debug/clk/clk_summary | grep -i lpuart`, is **16 MHz**.
Hardcoded `LPUART1_KER_CLK_RATE = 16000000` in `rcc_stub.c` and rebuilt.

**Result: hardware-validated.** After
`echo stop > /sys/class/remoteproc/remoteproc1/state` /
`echo start > /sys/class/remoteproc/remoteproc1/state` on the Linux side:
```
*** Booting Zephyr OS build v4.4.2 ***
hello, world 0
hello, world 1
hello, world 2
...
```
correctly appears on `/dev/ttySTM1`, and the combined
`samples/basic/blinky_hello` sample (new, added in this phase — LED
toggle + periodic "hello, world N" print, one per second) was built and
validated the same way.

## New sample: `samples/basic/blinky_hello`
A small sample combining `blinky` + `hello_world`: toggles the `led0`
GPIO and prints an incrementing `hello, world N` message once a second.
Useful as a quick smoke test that both GPIO output and the console UART
work together — this is what was actually flashed for the final hardware
validation. See `GETTING_STARTED.md` for exact build/flash commands.

## Files changed — quick index
See the patches for full diffs. Summary by repo:

**`zephyr` repo** (32 files, +676/-2 lines):
- New board files: `boards/st/stm32mp257f_dk/stm32mp257f_dk_stm32mp257fxx_m0plus.{dts,yaml,_defconfig}`,
  `boards/st/stm32mp257f_dk/support/openocd_stm32mp257f_dk_m0plus.cfg`
- New SoC files: `zephyr/soc/st/stm32/stm32mp2x/m0plus/{soc.c,soc.h,CMakeLists.txt,linker.ld,rcc_stub.c,reset_stub.c}`
- New devicetree: `dts/arm/st/mp2/{stm32mp257_m0plus.dtsi,stm32mp2_m0plus.dtsi}`,
  `dts/bindings/{clock/st,stm32mp2-rcc-m0stub.yaml,reset/st,stm32-rcc-rctl-m0stub.yaml}`
- New sample: `samples/basic/blinky_hello/`
- Modified shared files: `boards/st/stm32mp257f_dk/{Kconfig.stm32mp257f_dk,board.cmake}`,
  `drivers/interrupt_controller/intc_exti_stm32_priv.h` (real bug fix),
  `include/zephyr/dt-bindings/{clock,reset}/stm32mp2_{clock,reset}.h`,
  `soc/st/stm32/common/{gpioport_mgr.c,soc_config.c,stm32_backup_domain.c}`,
  `soc/st/stm32/{soc.yml,stm32mp2x/{CMakeLists.txt,Kconfig,Kconfig.defconfig.stm32mp257fxx,Kconfig.soc}}`

**`modules/hal/stm32` repo** (3 files, +42/-24 lines):
- `stm32cube/CMakeLists.txt` — `-DCORE_CM0PLUS` compile define
- `stm32cube/stm32mp2xx/CMakeLists.txt` — gate `system_stm32mp2xx_m33.c`/
  `_rcc.c`/`_rcc_ex.c` on core, add `system_stm32mp2xx_m0plus.c`
- `stm32cube/stm32mp2xx/drivers/include/stm32mp2xx_hal_conf.h` — gate
  ~28 HAL module enables behind `#if !defined(CORE_CM0PLUS)`
