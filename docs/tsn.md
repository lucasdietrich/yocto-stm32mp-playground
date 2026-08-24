Good instinct — stripping out the AI/NPU part lets you actually isolate and understand TSN mechanics without confounding variables. And yes, **two MP2 boards is a great setup** — arguably the right way to learn this, since TSN's value only shows up when there's real network traffic between two systems (not loopback).

## Two-board topology

```
[MP2 board A]  <---- Ethernet cable ---->  [MP2 board B]
   eth0                                        eth0
 (talker/                                   (listener/
  sender)                                    receiver)
```

Direct cable, no switch needed for basic tests (that's actually simpler — a plain point-to-point link removes the switch as a variable). Later, adding a TSN-capable switch between them lets you test multi-hop scheduling, which is a good "phase 2."

## A practical, staged learning progression

### Stage 0 — Baseline, no TSN at all
Get comfortable measuring latency/jitter honestly before touching any qdisc.

```bash
# Board B (listener):
ping board_A_ip

# Better: use a proper jitter/latency tool
iperf3 -s          # board B
iperf3 -c board_B_ip -u -b 10M -t 30   # board A, UDP so you get jitter stats
```
`iperf3 -u` reports jitter directly — this becomes your reference "before" number.

### Stage 1 — PTP synchronization first
TSN is meaningless without a shared time base, so get this rock solid before anything else.

```bash
# Both boards need ptp4l (from linuxptp package)
ptp4l -i eth0 -m --step_threshold=1

# Board A: master (or let BMCA decide automatically)
# Board B: slave, then sync system clock:
phc2sys -s eth0 -c CLOCK_REALTIME -w -m
```
Check sync quality:
```bash
pmc -u -b 0 'GET TIME_STATUS_NP'
```
This alone is a legitimate, non-trivial exercise: verifying offset staying within nanoseconds/low-microseconds between boards.

### Stage 2 — Background "noise" traffic generator
Before adding TSN scheduling, create the disruptive traffic you'll later prove TSN protects against.

```bash
# Board A: flood best-effort UDP traffic
iperf3 -c board_B_ip -u -b 900M -t 60 &
```

### Stage 3 — Add a "critical" low-rate stream competing with the noise
```bash
# Small, periodic UDP packets simulating a control/sensor stream
# A simple netcat/socat loop, or better: use cyclictest-style periodic sender
```
Measure its latency/jitter **while the noise stream is running**, with no TSN configured. This is your "problem" baseline — you should see the critical stream's latency spike/jitter badly under load.

### Stage 4 — Apply `taprio`/EST to isolate the critical stream
Now bring in what we already tested:
```bash
tc qdisc add dev eth0 parent root handle 100 taprio \
    num_tc 2 \
    map 0 0 0 0 1 1 1 1 \
    queues 2@0 2@2 \
    base-time 0 \
    sched-entry S 0x1 100000 \
    sched-entry S 0x2 100000 \
    flags 0x2
```
Then classify your critical traffic into the express gate's queue (via `tc filter` with `skbedit priority`, or set socket priority with `SO_PRIORITY` in your sender), and re-run Stage 3's measurement. **Same noise load, same critical stream — but now measure the difference.**

### Stage 5 — Add FPE for preemption
Once EST works, layer in frame preemption so large noise frames get interrupted mid-transmission instead of just scheduled around:
```bash
tc qdisc add dev eth0 parent root handle 100 taprio \
    ... \
    fp E P P P \
    flags 0x2 0x2
```
(exact syntax varies by kernel version — this is from the patch series we found earlier)

### Stage 6 — Introduce a switch, test multi-hop
Add a TSN-capable switch between the boards and repeat Stage 4/5 measurements. This tests whether your end-to-end schedule survives an extra hop — much closer to a real deployment.

## Why this order matters
Each stage isolates exactly one variable: sync, then load, then scheduling, then preemption, then topology. That way when something doesn't work, you know which layer broke it — which is honestly the biggest practical challenge with TSN debugging: symptoms at the application layer (jittery control loop) can come from PTP sync drift, qdisc misconfiguration, hardware EST rejection, or switch misconfiguration, and they all look the same from the outside.

Want me to write out the actual `tc filter`/socket-priority commands to correctly steer your "critical" test traffic into the express queue, since that's usually the fiddliest part to get right?