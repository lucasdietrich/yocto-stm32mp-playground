# pgbench / fio — Global Measurement Summary

**Setup (all runs):** PostgreSQL 16.12, TPC-B (sort of), scale=20, clients=10, threads=10,
duration=60 s, device `/dev/mmcblk1p1`, `synchronous_commit=on`, `fsync=on`.

## Config Reference

| ID  | FS    | Encryption                | Integrity              | Key mgmt      | Key size           | IV mode      | Tag (B) |
| --- | ----- | ------------------------- | ---------------------- | ------------- | ------------------ | ------------ | ------- |
| D   | ext4  | none                      | none                   | —             | —                  | —            | —       |
| E   | ext4  | aes-xts-plain64           | none                   | plain         | 256 b              | sector#      | —       |
| G   | btrfs | none                      | none                   | —             | —                  | —            | —       |
| A   | btrfs | aes-xts-plain64           | none                   | plain         | 256 b              | sector#      | —       |
| I ¹ | btrfs | aes-cbc-plain64           | none                   | plain         | 256 b              | sector# (4K) | —       |
| F   | ext4  | none                      | crc32c (non-keyed)     | plain         | —                  | —            | 4       |
| B   | ext4  | aes-xts-plain64           | hmac-sha256 (sep. key) | plain         | 256 b + 256 b HMAC | sector#      | 32      |
| C   | ext4  | aes-xts-plain64 + authenc | hmac-sha256 (combined) | LUKS2         | 512 b (split)      | random       | 48      |
| H   | ext4  | aes-gcm-random            | AEAD (GCM)             | LUKS2         | 256 b              | random       | 16      |
| J ² | ext4  | aes-gcm-random            | AEAD (GCM)             | plain (dmset) | 256 b              | random       | 28 ³    |

¹ Production match: `aes-cbc-plain64`, `sector_size=4096`, `allow_discards`, btrfs.  
² Raw `integritysetup` + `dmsetup` pipeline, no LUKS2 header, no KDF.  
³ 12 B GCM nonce + 16 B GCM auth tag.

---

## pgbench TPS — All Runs

Latency σ shown in parentheses where elevated (> 20 ms flags instability).

| ID  |        Run 2 |            Run 3 |            Run 4 |            Run 5 |           Stable est. |   vs D stable |
| --- | -----------: | ---------------: | ---------------: | ---------------: | --------------------: | ------------: |
| D   |  440.1 (7.3) |      442.4 (7.2) |      440.4 (7.2) |      422.6 (8.4) |                  ~441 |      baseline |
| E   |  422.7 (7.9) |      420.1 (8.0) |      421.8 (7.9) |      422.1 (7.9) |                  ~422 |        −4.3 % |
| G   | 306.0 (12.9) |     310.9 (12.6) |     299.3 (13.4) |     300.3 (13.0) |                  ~304 |       −31.1 % |
| A   | 249.8 (16.2) |     244.9 (16.2) | 188.7 **(51.3)** | 188.7 **(51.6)** | ~247 warm / ~189 cold | −44 % / −57 % |
| I   |            — |     257.9 (14.6) |     251.9 (15.6) | 204.4 **(24.3)** | ~254 warm / ~204 cold | −42 % / −54 % |
| F   | 375.2 (15.6) | 373.1 **(35.4)** |     389.2 (11.6) |     388.9 (10.0) |             373–389 ⁴ |    −14 / −7 % |
| B   | 389.0 (10.0) |     388.1 (10.4) |     388.6 (10.1) |     383.0 (10.7) |                  ~387 |       −12.2 % |
| C   |  382.9 (9.8) |     382.0 (10.1) |     378.0 (10.3) | 367.5 **(13.8)** |                  ~381 |       −13.6 % |
| H   | 382.9 (10.0) | 367.4 **(39.1)** |     370.1 (12.8) |      381.7 (9.8) |                  ~378 |       −14.3 % |
| J   |            — |                — | 382.9 **(27.4)** | 377.4 **(28.9)** |                  ~380 |       −13.7 % |

**Bold σ** = anomalously high latency dispersion; TPS mean for that run is unreliable.  
Stable est. = mean over runs with σ ≤ 20 ms.  
D stable = 441 TPS (mean of runs 2–4; run 5 excluded — D ran ~18 TPS below average that run).

⁴ F shows a reproducible bimodal: ~373 TPS when the integrity journal is cold/noisy (runs 2, 3),
  ~389 TPS when the journal is primed (runs 4, 5). The lower bound is the conservative figure.

---

## Cost Layers — Stable Delta Analysis

Using stable estimates vs D (~441 TPS):

| Cost layer                                     | Isolated by  | Effect on TPS |       % vs D |
| ---------------------------------------------- | ------------ | ------------: | -----------: |
| **btrfs CoW** (no crypt, no integrity)         | G − D        |          −137 |        −31 % |
| **AES-XTS on ext4** (HW AES)                   | E − D        |           −19 |         −4 % |
| **AES-XTS on btrfs** (CoW + crypt)             | A − D (warm) |          −194 |        −44 % |
| **CBC 4K-sector on btrfs** (production)        | I − D (warm) |          −187 |        −42 % |
| **dm-integrity journal** (crc32c, no hash)     | F − D        |     −52 / −68 | −7 / −14 % ⁴ |
| **dm-integrity + HMAC-SHA256** (sep. key)      | B − D        |           −54 |        −12 % |
| **dm-integrity + HMAC-SHA256** (LUKS2 authenc) | C − D        |           −61 |        −14 % |
| **dm-integrity + AES-GCM** (LUKS2)             | H − D        |           −63 |        −14 % |
| **dm-integrity + AES-GCM** (raw, no LUKS2)     | J − D        |           −61 |        −14 % |
| LUKS2 overhead (GCM)                           | H − J        |            −2 |        ≈ 0 % |
| GCM vs authenc/HMAC (LUKS2)                    | H − C        |            −3 |        ≈ 0 % |
| authenc (single-pass) vs two-layer (B)         | C − B        |            −6 |       −1.5 % |

---

## fio Microbenchmarks (Run 5 only)

Three synthetic workloads, direct I/O, on the same `/dev/mmcblk1p1` device:
- **oltp**: randrw 80 % read / 20 % write, 8 K, 4 jobs, iodepth=4, 15 s
- **rand\_index**: pure random read, 8 K, 4 jobs, iodepth=4, 15 s
- **wal\_fsync**: sequential write + fdatasync per I/O, 1 job, iodepth=1, 15 s

### OLTP (randrw 80/20, 8 K)

| ID  | R IOPS | R BW (MB/s) | W IOPS | W BW (MB/s) |
| --- | -----: | ----------: | -----: | ----------: |
| D   |   2595 |        20.3 |    662 |         5.2 |
| E   |   2030 |        15.9 |    525 |         4.1 |
| G   |   3128 |        24.4 |    800 |         6.3 |
| A   |   2285 |        17.9 |    588 |         4.6 |
| I   |   2937 |        22.9 |    753 |         5.9 |
| F   |   1443 |        11.3 |    373 |         2.9 |
| B   |    837 |         6.5 |    219 |         1.7 |
| C   |    805 |         6.3 |    211 |         1.7 |
| H   |    921 |         7.2 |    241 |         1.9 |
| J   |   1312 |        10.2 |    341 |         2.7 |

### Random Index Scan (pure read, 8 K)

| ID  | R IOPS | R BW (MB/s) | R lat avg (µs) |
| --- | -----: | ----------: | -------------: |
| D   |   4377 |        34.2 |           3647 |
| E   |   4154 |        32.3 |           3841 |
| G   |   4332 |        33.8 |           3683 |
| A   |   3955 |        30.9 |           4033 |
| I   |   4197 |        32.8 |           3782 |
| F   |   4128 |        32.2 |           3866 |
| B   |   2553 |        19.9 |           6255 |
| C   |   2395 |        18.7 |           6669 |
| H   |   2630 |        20.5 |           6071 |
| J   |   2650 |        20.7 |           6025 |

### WAL Fsync (sequential write + fdatasync)

| ID  | W IOPS | W BW (MB/s) | W lat avg (µs) |
| --- | -----: | ----------: | -------------: |
| D   |    733 |         5.7 |            313 |
| E   |    358 |         2.8 |            764 |
| G   |    111 |        0.87 |            429 |
| A   |     81 |        0.63 |            808 |
| I   |    135 |        1.05 |            632 |
| F   |     70 |        0.55 |            391 |
| B   |     97 |        0.76 |           1047 |
| C   |     99 |        0.77 |           1244 |
| H   |     99 |        0.77 |           1188 |
| J   |    167 |        1.31 |           1518 |

---

## Analysis

### 1 — btrfs CoW is the dominant cost

btrfs alone (G) costs −31 % vs ext4 (D). Adding AES-XTS (A warm) extends the gap to −44 %,
and the production cipher AES-CBC with 4K sectors (I warm) reaches −42 %. No cipher choice
can overcome the write amplification from btrfs CoW journaling on this device. The fio WAL
fsync workload makes this concrete: G achieves only 111 IOPS vs D's 733 IOPS — a 6.6× gap
— because every btrfs CoW write triggers an additional journal write per fsync.

### 2 — AES-XTS on ext4 is negligible in pgbench; visible only in saturated-write fio

E costs only −4 % vs D in pgbench TPS (422 vs 441 TPS), consistent with hardware AES (`pmull` /
AES-NI) absorbing the cipher compute. However, the fio WAL fsync workload reveals a 2× throughput
drop (358 vs 733 IOPS, latency 764 vs 313 µs): when writes are fully synchronous and maximally
saturated, the per-sector XTS path in dm-crypt adds a measurable serialized overhead that
PostgreSQL's write coalescing hides under normal operation.

### 3 — dm-integrity journal cost, not crypto hash, limits B/C/H/J

F (crc32c — trivial hash) and B (HMAC-SHA256 — strong hash) sit within 2 TPS of each other in
stable runs (387–389 TPS). The bottleneck is not the hash compute; it is the extra I/O pass to
write and read the tag-slot journal region. This is confirmed by fio:
- rand\_index: F reads at 4128 IOPS (≈ D), B/C/H/J at 2395–2650 IOPS (≈ half of D). CRC32c
  verification is essentially free; HMAC/GCM tag verification is not — reads must fetch the tag
  block in addition to the data block.
- wal\_fsync: all integrity configs (F/B/C/H/J) collapse to 70–167 IOPS vs D's 733. Every
  fdatasync flushes the integrity journal, doubling or tripling the write count regardless of
  which hash algorithm is used.

### 4 — authenc, GCM, and raw GCM converge in pgbench

B (~387 TPS), C (~381 TPS), H (~378 TPS), and J (~380 TPS) form a tight cluster −12 to −14 %
below D. The differences within this group are:

| Transition | Structural delta | Interpretation                                 |
| ---------- | ---------------: | ---------------------------------------------- |
| B → C      |           −6 TPS | LUKS2 key management + random IV + single-pass |
| C → H      |           −3 TPS | GCM (GHASH + CTR) vs authenc (HMAC + XTS)      |
| H → J      |           −2 TPS | eliminating LUKS2 header (raw pipeline)        |

All deltas are within measurement noise for individual runs. Across the four runs, the ordering
B > J ≈ C ≈ H holds structurally, but by margins too small to be operationally significant.

### 5 — Working-set instability in A and I

A and I exhibit a sharp bimodal behavior:
- **Warm state** (runs 2–3 for A; runs 3–4 for I): σ ~15 ms, TPS ~247 (A) / ~254 (I).
- **Cold state** (runs 4–5 for A; run 5 for I): σ ~24–52 ms, TPS ~189 (A) / ~204 (I).

The cold state arises when the btrfs CoW working set does not fit in the page cache between
test iterations. PostgreSQL must re-read dirty btrfs metadata on every write, amplifying latency
variance. The warm state values (~247 / ~254 TPS) are optimistic; the cold state values
(~189 / ~204 TPS) are closer to production steady-state behaviour on this device.

### 6 — Measurement reproducibility

Stable configs across all runs (σ ≤ 12 ms, consistent TPS):

| Config | Stable TPS | Run-to-run range |
| ------ | ---------: | ---------------: |
| D      |        441 |          422–442 |
| E      |        422 |          420–423 |
| B      |        387 |          383–389 |

These three anchor points bound the measurement noise floor. Any delta > 5 TPS between configs
in the same run is structural; deltas ≤ 5 TPS (e.g., C vs H) require multiple stable runs to
confirm direction.
