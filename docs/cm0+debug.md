# Debugging the Cortex-M0+ on STM32MP257F-DK with OpenOCD + GDB

## 1. Background: why the M0+ can't run "alone"

The STM32MP25x boot ROM only knows how to boot one primary core, selected by
OTP/board config:

- **CA35TDCID mode** - the Cortex-A35 is the boot master (TF-A -> OP-TEE ->
  U-Boot -> Linux). This is the normal OpenSTLinux setup.
- **CM33TDCID mode** - the Cortex-M33 is the boot master instead, for pure
  bare-metal/MCU-style projects with no Linux at all.

There is no boot mode where the Cortex-M0+ is the primary/boot-capable core.
It is always a secondary coprocessor: at reset it is held in its own local
reset domain with its clock gated, until RCC "hold boot" is released by
software running on the A35 or the M33. This is why:

- Cold-booting the board with nothing running on A35/M33 leaves the m0p
  OpenOCD target stuck in `state: reset`, and `halt` on `gdb-attach` times
  out (`Error: timed out while waiting for target halted`). The core simply
  cannot respond to a debug halt request while it has no clock and is held
  in reset.
- `monitor reset halt` from a GDB session attached only to the m0p target
  still fails while Linux is running on the A35, because OpenOCD's `reset`
  command is adapter-wide, not per-target: it walks every configured target
  (`a35_0`, `a35_1`, `m33`, `m0p`) and resetting a live Cortex-A35 running
  Linux isn't defined in the stock config, so it aborts with
  `stm32mp25x.a35_0: how to reset?` before ever reaching m0p.

## 2. Two ways to get code onto the M0+

### 2a. Standalone SRAM load via GDB `load` (no OS involved)

Works if you just need to get *some* firmware running for a quick bring-up
test, but has caveats:

- The primary core (A35 or M33, per boot mode) still has to run first to
  release the M0+ from reset. There is no way around this at cold boot.
- A shared `NRST` toggles the whole chip, wiping out anything you loaded.
  Don't reset the board with the reset button mid-session; if you need a
  restart, do it from GDB (section 3) instead.

### 2b. Deploying via Linux remoteproc (recommended once Linux is up)

Once OpenSTLinux is booted, the standard way to load/control the M0+ is the
kernel's remoteproc framework:

```bash
# find the right instance
cat /sys/class/remoteproc/remoteproc*/name

# stop, point at firmware under /lib/firmware, start
echo stop           > /sys/class/remoteproc/remoteproc1/state
echo my_fw.elf       > /sys/class/remoteproc/remoteproc1/firmware
echo start           > /sys/class/remoteproc/remoteproc1/state
```

`stop` puts the core back through RCC hold-boot/reset. `start` releases it
and re-parses the resource table (carveouts, vrings, trace buffers).

Important: once a GDB session is attached and manipulating the core (SP/PC,
breakpoints, memory), remoteproc has no idea. Don't mix `echo stop`/`start`
into the same core while GDB also believes it owns it - stop touching one
side before switching to the other, except for the controlled workflow in
section 4.

## 3. Restarting the firmware from GDB, without touching A35/M33

### Option A - software-only restart (recommended, always safe)

Manually replay what the CPU does on reset: load SP from word 0 of the
vector table and PC from word 1, then continue. This re-runs
`Reset_Handler`, so clock config, `.data`/`.bss` init and `HAL_Init` all run
correctly (jumping straight to `main` would skip that and is not
equivalent to a real reset).

```
(gdb) load
(gdb) set $sp = *(unsigned int *)0x200c0000
(gdb) set $pc = *(unsigned int *)0x200c0004
(gdb) continue
```

Replace `0x200c0000` with your vector table base if different. This never
touches SRST or any hardware reset line, so it cannot disturb the A35 or
M33 regardless of how their reset domains are wired.

### Option B - `monitor reset halt` (does NOT work with Linux running)

```
(gdb) monitor reset halt
```

Fails as described in section 1 once Linux is live on the A35. Not usable
in this setup; kept here only for reference on simpler (single-core)
STM32 parts where it works fine.

### Loading a different ELF entirely

```
(gdb) file /path/to/new_firmware.elf
(gdb) load
(gdb) set $sp = *(unsigned int *)0x200c0000
(gdb) set $pc = *(unsigned int *)0x200c0004
(gdb) continue
```

## 4. Setting a breakpoint before the firmware even starts (vector catch)

Goal: set a breakpoint (e.g. at `main`), then trigger a fresh start from
Linux (`echo start`), and actually have GDB stop there - rather than the
core being reset out from under GDB.

The problem: `echo stop` puts the M0+ back into RCC reset. Breakpoints are
implemented via the Flash Patch and Breakpoint (FPB) unit inside the core,
which is not usable while the core is held in reset - hence
`Cannot insert breakpoint` / `target not halted` errors if you try to set
breakpoints right after a `stop`.

The fix: Cortex-M's DEMCR register has a `VC_CORERESET` bit ("vector
catch") that halts the core immediately after reset is deasserted, before
it executes a single instruction - independent of the FPB unit. OpenOCD
exposes this as `cortex_m vector_catch`.

Sequence:

```
(gdb) monitor cortex_m vector_catch reset
```

```bash
# from Linux shell (or ssh):
echo stop  > /sys/class/remoteproc/remoteproc1/state
echo start > /sys/class/remoteproc/remoteproc1/state
```

The instant `start` deasserts the M0+ reset, OpenOCD catches it and halts
the core - clocked, accessible, and with a valid FPB unit again. Now:

```
(gdb) break main
(gdb) continue
```

works normally.

Cleanup: vector_catch is sticky and will keep halting the core on every
subsequent reset until cleared:

```
(gdb) monitor cortex_m vector_catch none
```

Keep a single continuous GDB session across the `stop`/`start` cycle rather
than disconnecting/reconnecting mid-sequence - reconnecting while the core
is mid-reset is what causes a cascade of `not halted` breakpoint errors.

Note: `monitor cortex_m vector_catch ...` is not a GDB feature. `monitor`
is GDB's passthrough prefix; `cortex_m vector_catch` is a native OpenOCD
command. It works identically typed directly into OpenOCD's telnet console
(port 4444) or placed in a `.cfg` file - the only difference is that on the
shared telnet console you must first select the target
(`targets stm32mp25x.m0p`), whereas GDB's `monitor` already implies the
target from which gdb port (3333/3334/3335) you connected to.

## 5. Consolidated OpenOCD startup

See `mp25_m0p.cfg`. Replaces:

```bash
openocd -f interface/stlink.cfg -c "transport select swd" -f target/st/stm32mp25x.cfg
```

with:

```bash
openocd -f mp25_m0p.cfg
```

It also defines two convenience procs for the telnet console:
`m0p_catch_reset` and `m0p_catch_none`.

## 6. Automated build -> deploy -> break-at-main script

See `debug_m0p.sh`. Given a project directory and board SSH details, it:

1. Builds the firmware (`make`).
2. Copies the ELF to the board's `/lib/firmware/`.
3. Stops the M0+ remoteproc instance and points it at the new firmware.
4. Arms `cortex_m vector_catch reset` over the m0p gdb port.
5. Restarts the M0+ remoteproc instance (core halts immediately at reset).
6. Launches an interactive GDB session, clears vector_catch, sets
   `break main`, and continues - landing exactly at `main()` with a fresh
   load every time.

Edit the configuration block at the top of the script (paths, board
IP/user, remoteproc instance name, gdb port) before use.

## 7. Quick command cheat sheet

| Goal                                            | Command |
|--------------------------------------------------|---------|
| Start OpenOCD                                     | `openocd -f mp25_m0p.cfg` |
| Restart firmware from GDB (safe, core-only)       | `set $sp = *(unsigned int *)0x200c0000` / `set $pc = *(unsigned int *)0x200c0004` / `continue` |
| Reset just the M0+ from Linux                     | `echo stop > .../state` then `echo start > .../state` |
| Load different firmware via remoteproc            | `echo new.elf > .../firmware` (while stopped) |
| Halt at very first instruction after reset        | `monitor cortex_m vector_catch reset` before triggering the reset |
| Stop catching future resets                       | `monitor cortex_m vector_catch none` |
| Reset "just m0p" via OpenOCD `monitor reset`      | Does not work while Linux runs on A35 - use the GDB-side restart instead |