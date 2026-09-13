# Getting Started — STM32MP257F-DK Cortex-M0+ Zephyr Port

This guide walks through setting up a Zephyr workspace, applying the two
patches in `patches/`, and building + flashing the `blinky` and
`blinky_hello` samples for the M0+ coprocessor ("CPU3") of an
STM32MP257F-DK board.

It assumes:
- You already have the **amy** distribution's `amy-image` (built via
  Yocto/OpenEmbedded) flashed and booted on the board's Cortex-A35,
  reachable over SSH (or serial console), with `remoteproc` support for
  the M0+ core enabled.
- Host machine: Linux, with `west`, a Zephyr SDK, and Python 3.12+
  available (standard Zephyr getting-started prerequisites).

## 1. Create the west workspace

```sh
mkdir -p ~/zephyrproject-m0plus && cd ~/zephyrproject-m0plus
python3 -m venv .venv
source .venv/bin/activate
pip install west
west init -m https://github.com/zephyrproject-rtos/zephyr --mr v4.4.2 .
west update
west zephyr-export
pip install -r zephyr/scripts/requirements.txt
```

Expected result: a workspace with `zephyr/`, `modules/`, `bootloader/`,
`tools/` etc. populated, and no errors from `west update`.

If you don't already have a Zephyr SDK installed:
```sh
cd zephyr
west sdk install
```

## 2. Apply the patches

Copy `patches/0001-zephyr-stm32mp257f-dk-cortex-m0plus-port.patch` and
`patches/0002-hal_stm32-stm32mp257f-dk-cortex-m0plus-port.patch` into your
new workspace, then:

```sh
cd ~/zephyrproject-m0plus/zephyr
git apply --check /path/to/0001-zephyr-stm32mp257f-dk-cortex-m0plus-port.patch
git apply /path/to/0001-zephyr-stm32mp257f-dk-cortex-m0plus-port.patch

cd ~/zephyrproject-m0plus/modules/hal/stm32
git apply --check /path/to/0002-hal_stm32-stm32mp257f-dk-cortex-m0plus-port.patch
git apply /path/to/0002-hal_stm32-stm32mp257f-dk-cortex-m0plus-port.patch
```

`git apply --check` should produce no output (success). If it reports
conflicts, your checked-out `zephyr`/`hal_stm32` revision has diverged
from what these patches were generated against (`zephyr` tag `v4.4.2`,
`hal_stm32` module-manifest revision `fc11896d`) — pin those exact
revisions in `west.yml`/`west update` first.

## 3. Build `blinky` (GPIO-only smoke test)

```sh
cd ~/zephyrproject-m0plus
west build -p always -b stm32mp257f_dk/stm32mp257fxx/m0plus \
  zephyr/samples/basic/blinky
```

Expected output (tail):
```
Memory region         Used Size  Region Size  %age Used
           FLASH:       10624 B        16 KB     64.84%
             RAM:          2 KB         8 KB     25.00%
```

## 4. Build `blinky_hello` (GPIO + LPUART1 console)

```sh
west build -p always -b stm32mp257f_dk/stm32mp257fxx/m0plus \
  zephyr/samples/basic/blinky_hello
```

Expected output (tail):
```
Memory region         Used Size  Region Size  %age Used
           FLASH:       13500 B        16 KB     82.40%
             RAM:          2 KB         8 KB     25.00%
```

## 5. Copy the firmware to the target

```sh
scp build/zephyr/zephyr.elf root@<board-ip>:/lib/firmware
```

(Substitute whatever `remoteproc` firmware-search path your image uses —
`/lib/firmware` is the default for `amy-image`.)

## 6. Start/restart the M0+ coprocessor from Linux

On the board (over SSH/serial):
```sh
echo zephyr.elf > /sys/class/remoteproc/remoteproc1/firmware
echo stop  > /sys/class/remoteproc/remoteproc1/state   # only if already running
echo start > /sys/class/remoteproc/remoteproc1/state
```
(`remoteproc1` is the M0+ instance on this board/image — check
`cat /sys/class/remoteproc/remoteproc*/name` if unsure which index it is.)

## 7. Observe the console

Why `/dev/ttySTM1` shows the M0+'s output at all: the M0+ firmware only
has access to **LPUART1**, whose TX pin is **PZ7** (GPIOZ bank, AF6,
`lpuart1_tx_pz7`). LPUART1 is not a UART Linux drives itself here —
instead, the STM32MP257F-DK board wires PZ7 on the PCB straight into
**PF14**, which is the RX pin of **USART6** (GPIOF bank, AF3,
`usart6_rx_pf14`), a completely different peripheral instance that
belongs to the main power domain and *is* enabled/owned by Linux. Linux's
STM32 serial driver enumerates it as `/dev/ttySTM1`. So the path is:
M0+ core → LPUART1 TX (PZ7) → PCB trace → USART6 RX (PF14) → Linux
USART6 driver → `/dev/ttySTM1`. PZ7 and PF14 are two unrelated silicon
pads on two unrelated peripherals; they are only connected via the board
wiring, not internally. This is also **TX-only**: nothing on this board
wires USART6_TX back to LPUART1_RX (PZ8), so there is no way to send
data from Linux to the M0+ over this same link — `cat` works, sending
input to the M0+ does not.

`/dev/ttySTM1` comes up with whatever line settings the tty last had, so
configure it explicitly before reading (match the LPUART1 config in the
board `.dts`: 115200 8N1, no flow control, and `raw`/`-echo` so the
terminal driver doesn't mangle bytes):
```sh
stty -F /dev/ttySTM1 115200 cs8 -cstopb -parenb raw -echo
cat /dev/ttySTM1
```

Expected output for `blinky_hello`:
```
*** Booting Zephyr OS build v4.4.2 ***
hello, world 0
hello, world 1
hello, world 2
...
```
one new line per second, with the PZ0 (GPIOZ pin 0) LED/test pin toggling
in sync. `blinky` alone (Phase 2a, no console wired) only toggles PZ0 and
produces no console output — verify it with a multimeter/scope/logic
analyzer on PZ0, or flash `blinky_hello` instead if you want a console
signal to confirm liveness.

`Ctrl-C` to stop watching `cat`; this does not stop the M0+ firmware
(use the `remoteproc` `stop` command above for that).

## 8. (Optional) Debug with OpenOCD + GDB

```sh
west debug
```
or manually:
```sh
openocd -f interface/stlink.cfg -c "transport select swd" \
        -f target/st/stm32mp25x.cfg
```
then in another terminal:
```sh
arm-none-eabi-gdb -q \
  -ex "target extended-remote localhost:3335" \
  -ex "b main" \
  build/zephyr/zephyr.elf
```
GDB ports opened by `target/st/stm32mp25x.cfg`: `3333` -> `stm32mp25x.a35_0`,
`3334` -> `stm32mp25x.m33`, **`3335` -> `stm32mp25x.m0p`** (confirmed on
hardware; this is what `board.cmake` uses for `west debug`).

`monitor reset halt` does **not** work once Linux is running on the A35
(it tries to reset all targets including the live `a35_0`). Instead, the
M0+ support config
(`boards/st/stm32mp257f_dk/support/openocd_stm32mp257f_dk_m0plus.cfg`)
automatically arms `cortex_m vector_catch reset` on the `stm32mp25x.m0p`
target as soon as OpenOCD examines it, so the core halts at the very
first instruction after any reset — including a Linux-side `echo start` —
with no manual step needed. It's sticky across resets; disable it for a
session with `monitor cortex_m vector_catch none` if you don't want it.

## Troubleshooting

- **Nothing on `/dev/ttySTM1`, ever**: check the resource-manager/RIF
  side has actually assigned LPUART1's clock domain to M0+ before start
  (this port assumes the A35/Linux side already turns the LPUART1 clock
  on — it is never enabled by the M0+ firmware itself, see
  `OPEN_POINTS.md`).
- **Garbage/repeating single byte on the console**: baud-rate mismatch.
  Verify the real `ck_ker_lpuart1` rate on your specific board/image with
  `cat /sys/kernel/debug/clk/clk_summary | grep -i lpuart` and update
  `LPUART1_KER_CLK_RATE` in
  `zephyr/soc/st/stm32/stm32mp2x/m0plus/rcc_stub.c` if it differs from
  the `16000000` hardcoded here (see `OPEN_POINTS.md` — this value is
  **not guaranteed to be the same across boards/BSP versions/boot
  configurations**).
- **Build error mentioning `undefined node label 'pinctrl'`**: you're
  probably missing the `#include <st/mp2/stm32mp257faix-pinctrl.dtsi>` in
  your board `.dts`, or applied the patches out of order/incompletely.
- **`west build` can't find board `stm32mp257f_dk/stm32mp257fxx/m0plus`**:
  confirm both patches applied cleanly (step 2) — the M0+ board qualifier
  is added entirely by patch 0001.
