# Open Points — STM32MP257F-DK Cortex-M0+ Zephyr Port

These are the questions and gaps that should be discussed/resolved before
this port is considered production-ready or proposed for upstreaming.

## 1. Is Zephyr the right choice for a 16 KB/8 KB target?

`blinky_hello` (GPIO + interrupt-driven UART console, printf) already
uses **82% of the 16 KB code budget** and 25% of the 8 KB RAM budget —
and that's before adding any actual application logic (the IPC channel
to Linux, sensor/actuator handling, whatever the M0+ is meant to do in
production). Concretely, on this budget:
- There is effectively no room for more than one more small feature
  (e.g. a basic IPCC-based IPC handler) before FLASH is exhausted.
- Zephyr's kernel overhead (scheduler, printf/formatting via picolibc,
  device model, devicetree-derived static structures) is proportionally
  very large for a target this small — most of that 13.5 KB is kernel
  and libc, not application code.
- Interrupt-driven UART (`CONFIG_UART_INTERRUPT_DRIVEN`) pulls in more
  code than a polling/busy-wait UART driver would. If console output is
  only needed for occasional debug prints rather than a real always-on
  UART consumer, a poll-based or even a raw register-level "debug UART"
  approach might be far cheaper.

**Discussion needed**: what's the actual planned application workload for
this M0+ core in production?
- If it's genuinely minimal (e.g. "wake on GPIO edge, notify A35 over
  IPCC, go back to low power"), a **bare-metal or bare-CMSIS approach**
  (like ST's own `CM0PLUS_DEMO` reference) is likely the better fit —
  full control over code size, no kernel/scheduler overhead, and it's
  what ST's own tooling/examples are built around.
- If the workload will grow (multiple peripherals, more complex logic,
  need for real driver abstractions/portability across future
  ST/other-vendor SoCs), Zephyr's abstractions pay for themselves, but
  the LPSRAM budget (fixed at 32 KB total by hardware, not configurable)
  becomes the hard ceiling — worth sizing the actual target application
  against this budget *before* committing further engineering time to
  Zephyr on this core, since it might not fit at all.
- A middle ground: use Zephyr but aggressively trim it
  (`CONFIG_MINIMAL_LIBC` instead of picolibc, disable
  `UART_INTERRUPT_DRIVEN` in favor of polling, `CONFIG_PRINTK` instead of
  full `printf`/picolibc formatting, no shell, minimal thread count) —
  not yet attempted/measured in this port.

## 2. Porting-methodology issues (RCC abstraction and friends)

- **The RCC/reset "stub driver" pattern is a workaround, not a real
  clock-control implementation.** `rcc_stub.c` always reports "clock is
  on" and returns a single hardcoded rate
  (`LPUART1_KER_CLK_RATE = 16000000`) regardless of which peripheral's
  clock is actually being queried. This *happens* to be correct today
  because LPUART1 is the only clock consumer wired up so far, but:
  - It is **silently wrong** for any other peripheral added later unless
    someone remembers to special-case it in `rcc_stub.c`. There is no
    compile-time or runtime check that would catch a future peripheral
    silently getting the wrong clock rate — it would just misbehave
    exactly like the LPUART1 baud-mismatch bug did in Phase 2c, and might
    be much harder to diagnose for e.g. a timer or SPI peripheral than
    for a human-readable UART stream.
  - **Needs discussion**: should `rcc_stub.c` be extended to dispatch on
    the `clock_control_subsys_t` argument and maintain a small
    per-peripheral rate table (looked up once from real hardware via
    `clk_summary`, hardcoded per peripheral)? This is more robust but
    still fundamentally a maintenance burden — every new peripheral
    needs its real kernel-clock rate manually verified on real hardware
    via Linux `clk_summary`, since M0+ can never read it itself.
  - Longer-term/more-correct alternative: have the **A35/Linux side**
    expose the actual clock rate(s) to the M0+ firmware at
    runtime (e.g. via the IPCC channel, or a shared-memory resource
    table entry populated by `remoteproc` at load time) instead of
    hardcoding it in Zephyr source. Not implemented — would need
    coordination with whatever Linux-side firmware-loading/resource-table
    tooling is used for this SoC's remoteproc integration.
- **No RIF/resource-manager coordination check at boot.** Nothing in the
  M0+ firmware verifies that the A35/OP-TEE side has actually granted it
  ownership of LPUART1 (or GPIOZ) via RIF before touching those
  registers — ST's own demo does this via
  `ResMgr_Request(RESMGR_RESOURCE_RIFSC, STM32MP25_RIFSC_LPUART1_ID)`
  before any LPUART1 access. If the resource manager hasn't actually
  granted the M0+ core ownership, direct register access would trigger
  an IAC/bus fault — this port relies entirely on the *board's Linux
  device tree* being configured correctly ahead of time, with no
  in-firmware verification or graceful failure path.
- **Stub compatible strings vs. real M33/A35 bindings**: `rcc_stub.c`/
  `reset_stub.c` use dedicated new compatible strings
  (`st,stm32mp2-rcc-m0stub`, `st,stm32-rcc-rctl-m0stub`) rather than
  reusing/gating the real `st,stm32mp2-rcc`/`st,stm32-rcc-rctl` drivers
  behind a `CORE_CM0PLUS` check. This was a deliberate lower-blast-radius
  choice, but it means the M0+ devicetree's `rcc`/`rctl` nodes are
  *not* semantically equivalent to M33's/A35's — anyone reading the M0+
  board `.dts` cold could reasonably assume `&rcc`/`&rctl` behave like a
  real clock/reset controller. **Discussion needed**: is a code comment
  sufficient, or should these be renamed (e.g. `rcc_m0stub:`/
  `rctl_m0stub:`) to make the distinction unmistakable in the devicetree
  itself, at the cost of diverging further from the M33 node-naming
  convention?
- **`USART1=LPUART1` compile-definition hack**: functionally correct
  (verified: the `#if defined(USARTn)` guard only checks *existence*, and
  no other M0+ code references a bare `USART1` symbol), but this kind of
  "trick a vendor header's feature-detection macro" fix is fragile against
  future ST HAL header changes (e.g. if a future STM32Cube MP2 HAL update
  adds code inside that guard that assumes `USART1` really is a full
  USART, not an LPUART, aliased pointer). Worth a comment/TODO upstream
  if this ever gets proposed to Zephyr mainline, and worth re-checking
  after any `modules/hal/stm32` version bump.

## 3. Remaining work / phases not yet done

Referencing the phase plan from the start of this port:

- [x] Phase 0 — Research or vendor demo / debug notes
- [x] Phase 1 — `hello_world` boots on M0+ (LPSRAM-only, no console)
- [x] Phase 2a — Blinky (GPIOZ PZ0), hardware-validated
- [x] Phase 2b — LPUART1 console wired up, builds cleanly
- [x] Phase 2c — LPUART1 clock-rate bug found & fixed, **hardware-validated**
- [ ] **IPC with Linux over IPCC2** — ST's own demo uses raw `HAL_IPCC`
  (not OpenAMP/rpmsg) plus a flat shared-memory buffer; the port plan
  from the start of this work recommended the same approach for Zephyr
  (a plain mbox/ipm driver on ipcc2 + `zephyr,memory-region`, skipping
  full OpenAMP virtio/rpmsg as too heavy for the 32 KB budget). **Not
  started.**
- [ ] **RIF/resource-manager coordination** (see open point above) —
  no in-firmware check that M0+ actually owns the peripherals it touches.
- [ ] **Low-power / WFI errata handling** — ST's demo has a
  `lowpower_wa.c` handling an M0+-specific errata (must WFI <800 µs on
  EXTI2 lines 59/60 around D1/D2 power-transition events triggered by the
  main CPU). Not evaluated or ported — currently irrelevant since this
  port doesn't yet touch any low-power transitions, but will matter the
  moment the M0+ firmware needs to survive the A35/M33 entering deeper
  sleep states.
- [ ] **Code-size trimming** (see open point 1) — no attempt yet to
  measure/reduce the Zephyr kernel+libc footprint below the current
  ~82% FLASH usage for `blinky_hello`; budget is tight enough that this
  should happen before adding IPC or any other feature.
- [x] **OpenOCD/GDB M0+ debug config verified** — the GDB port for the
  `stm32mp25x.m0p` target is confirmed to be `3335`
  (`3333`->`a35_0`, `3334`->`m33`, `3335`->`m0p`), wired into
  `board.cmake`, and the M0+ support config now auto-arms
  `cortex_m vector_catch reset` on `examine-end` so no manual step is
  needed after a Linux-side `remoteproc` restart.
- [ ] **Upstream candidacy** — two of the fixes found along the way look
  like genuine, general Zephyr bugs independent of this specific board
  (the `CPU_CORTEX_M0` vs. `CPU_CORTEX_M0PLUS` Kconfig-check bug in
  `intc_exti_stm32_priv.h`, and the `gpioport_mgr.c` unconditional
  `DEVICE_DT_GET(DT_NODELABEL(rcc))` crash on any SoC without an `rcc`
  node) — worth splitting out and proposing to upstream Zephyr
  independently of the rest of this M0+-specific port, since they're
  small, self-contained, and improve robustness for any future SoC in a
  similar situation, not just this one.
- [ ] **No automated test/CI coverage** — everything here was verified
  by manual `west build` + manual flash/observe on one physical board.
  No `twister` testcase, no CI job, no regression protection if a future
  `hal_stm32`/`zephyr` version bump breaks any of the fragile bits above
  (the `USART1=LPUART1` trick and the hardcoded `LPUART1_KER_CLK_RATE`
  being the most likely to silently break).
