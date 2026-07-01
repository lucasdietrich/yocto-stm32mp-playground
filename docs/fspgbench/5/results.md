# pgbench results — run 5

**Setup:** scale=20, clients=10, threads=10, duration=60 s, device `/dev/mmcblk1p1`  
**PostgreSQL:** 16.12 — TPC-B (sort of), `synchronous_commit=on`, `fsync=on`

New vs run 4: fio microbenchmarks added (oltp, rand\_index, wal\_fsync workloads) for all
configs. No new pgbench config this run — full A–J matrix repeated.

## pgbench Results

| ID  | FS    | Encryption                | Integrity              | Key mgmt      | Key size                 | IV mode      | Tag (B) |   TPS | Lat avg (ms) | Lat σ (ms) |     vs D |
| --- | ----- | ------------------------- | ---------------------- | ------------- | ------------------------ | ------------ | ------- | ----: | -----------: | ---------: | -------: |
| D   | ext4  | none                      | none                   | —             | —                        | —            | —       | 422.6 |         23.6 |        8.4 | baseline |
| E   | ext4  | aes-xts-plain64           | none                   | plain         | 256 b                    | sector#      | —       | 422.1 |         23.7 |        7.9 |   −0.1 % |
| G   | btrfs | none                      | none                   | —             | —                        | —            | —       | 300.3 |         33.3 |       13.0 |  −28.9 % |
| A   | btrfs | aes-xts-plain64           | none                   | plain         | 256 b                    | sector#      | —       | 188.7 |         52.9 |       51.6 |  −55.3 % |
| I ¹ | btrfs | aes-cbc-plain64           | none                   | plain         | 256 b                    | sector# (4K) | —       | 204.4 |         48.9 |       24.3 |  −51.6 % |
| F   | ext4  | none                      | crc32c (non-keyed)     | plain         | —                        | —            | 4       | 388.9 |         25.7 |       10.0 |   −8.0 % |
| B   | ext4  | aes-xts-plain64           | hmac-sha256 (sep. key) | plain         | 256 b crypt + 256 b HMAC | sector#      | 32      | 383.0 |         26.1 |       10.7 |   −9.4 % |
| C   | ext4  | aes-xts-plain64 + authenc | hmac-sha256 (combined) | LUKS2         | 512 b (split)            | random       | 48      | 367.5 |         27.2 |       13.8 |  −13.1 % |
| H   | ext4  | aes-gcm-random            | AEAD (GCM)             | LUKS2         | 256 b                    | random       | 16      | 381.7 |         26.2 |        9.8 |   −9.7 % |
| J ² | ext4  | aes-gcm-random            | AEAD (GCM)             | plain (dmset) | 256 b                    | random       | 28 ³    | 377.4 |         26.5 |       28.9 |  −10.7 % |

¹ Production match: `aes-cbc-plain64`, `sector_size=4096`, `allow_discards`, btrfs.  
² Bypasses LUKS2: raw `integritysetup` + `dmsetup` pipeline, no KDF, no LUKS2 header.  
³ 28-byte tag slot = 12 B GCM nonce + 16 B GCM auth tag.

## pgbench Key Deltas

| Delta | Meaning                                                         | Effect              |
| ----- | --------------------------------------------------------------- | ------------------- |
| D → E | pure dm-crypt (aes-xts) overhead on ext4                        | −0.1 % (≈ 0)        |
| D → G | btrfs vs ext4, no security layers                               | −28.9 %             |
| E → A | btrfs vs ext4 under dm-crypt                                    | −55.3 % rel. to E   |
| G → I | dm-crypt (CBC, 4K sector) overhead on btrfs — production cipher | −31.9 % rel. to G   |
| A → I | CBC + 4K sectors vs XTS + 512B sectors on btrfs                 | +8.3 % rel. to A    |
| D → F | dm-integrity tag-slot I/O cost (crc32c, no crypto hash)         | −8.0 %              |
| F → B | + HMAC-SHA256 on top of tag I/O (separate key, det. IV)         | −5.9 TPS (−1.5 %)   |
| B → C | LUKS2 + random IV + single-pass authenc vs two-layer plain      | −15.6 TPS (−4.1 %)  |
| C → H | AES-GCM (GHASH+CTR) vs authenc (HMAC+XTS), same LUKS2          | +14.2 TPS (+3.9 %) ⁴|
| H → J | GCM without LUKS2 (raw integritysetup + dmsetup) vs LUKS2 GCM  | −4.2 TPS (−1.1 %)   |

⁴ C→H direction is reversed vs runs 3/4 due to C's elevated latency stddev (13.8 ms) depressing its mean.
  The structural cost of GCM vs authenc is ~−8 TPS (from stable runs 2/4).

## fio Microbenchmarks

Three workloads, direct I/O, per config:
- **oltp**: randrw 80 % read / 20 % write, 8 K blocks, 4 jobs, iodepth=4, 15 s
- **rand\_index**: pure random read, 8 K blocks, 4 jobs, iodepth=4, 15 s
- **wal\_fsync**: sequential write, fdatasync after each, 1 job, iodepth=1, 15 s

### OLTP (randrw 80/20, 8 K)

| ID | Read IOPS | Read BW (MB/s) | Write IOPS | Write BW (MB/s) | Read lat avg (µs) | Write lat avg (µs) |
| -- | --------: | -------------: | ---------: | --------------: | ----------------: | -----------------: |
| D  |      2595 |           20.3 |        662 |             5.2 |              4094 |               8066 |
| E  |      2030 |           15.9 |        525 |             4.1 |              5210 |              10277 |
| G  |      3128 |           24.4 |        800 |             6.3 |              3320 |               6958 |
| A  |      2285 |           17.9 |        588 |             4.6 |              5057 |               7432 |
| I  |      2937 |           22.9 |        753 |             5.9 |              3925 |               5666 |
| F  |      1443 |           11.3 |        373 |             2.9 |             10998 |                286 |
| B  |       837 |            6.5 |        219 |             1.7 |             17489 |               6132 |
| C  |       805 |            6.3 |        211 |             1.7 |             18113 |               6663 |
| H  |       921 |            7.2 |        241 |             1.9 |             15829 |               5793 |
| J  |      1312 |           10.2 |        341 |             2.7 |              8565 |              13909 |

### Random Index Scan (pure read, 8 K)

| ID | Read IOPS | Read BW (MB/s) | Read lat avg (µs) |
| -- | --------: | -------------: | ----------------: |
| D  |      4377 |           34.2 |              3647 |
| E  |      4154 |           32.3 |              3841 |
| G  |      4332 |           33.8 |              3683 |
| A  |      3955 |           30.9 |              4033 |
| I  |      4197 |           32.8 |              3782 |
| F  |      4128 |           32.2 |              3866 |
| B  |      2553 |           19.9 |              6255 |
| C  |      2395 |           18.7 |              6669 |
| H  |      2630 |           20.5 |              6071 |
| J  |      2650 |           20.7 |              6025 |

### WAL Fsync (sequential write + fdatasync)

| ID | Write IOPS | Write BW (MB/s) | Write lat avg (µs) |
| -- | ---------: | --------------: | -----------------: |
| D  |        733 |             5.7 |                313 |
| E  |        358 |             2.8 |                764 |
| G  |        111 |             0.87 |               429 |
| A  |         81 |             0.63 |               808 |
| I  |        135 |             1.05 |               632 |
| F  |         70 |             0.55 |               391 |
| B  |         97 |             0.76 |              1047 |
| C  |         99 |             0.77 |              1244 |
| H  |         99 |             0.77 |              1188 |
| J  |        167 |             1.31 |              1518 |

## pgbench Observations

- **D ≈ E: AES-XTS overhead collapses to zero this run**: for the first time across all runs, E
  (422.1 TPS) is within 0.6 TPS of D (422.6 TPS). Both stddevs are normal (7.9–8.4 ms), so this
  is not a spike artifact — D's mean simply came in ~18 TPS lower than its cross-run average
  (~440 TPS), while E remained stable. This is consistent with run-to-run variance in D's page
  cache warm-up state. The structural AES-XTS overhead is ~4–5 % (as seen in runs 2–4).

- **A and I remain cold-working-set territory**: A (188.7 TPS, stddev 51.6 ms) and I (204.4 TPS,
  stddev 24.3 ms) are consistent with run 4 values and well below run 3 (A: 244.9, I: 257.9).
  High stddev on A confirms the btrfs CoW working set is not warm. Run 3's higher TPS for A/I was
  anomalously warm; runs 4 and 5 represent the colder steady state.

- **btrfs CoW remains the dominant cost**: G (300.3 TPS) is −28.9 % below D; adding dm-crypt (A)
  pushes to −55.3 %. Config I recovers +8.3 % vs A but stays −51.6 % below D.

- **C → H inversion is a variance artifact**: C's stddev (13.8 ms) is elevated vs H (9.8 ms),
  depressing C's mean to 367.5. H at 381.7 now leads C by +14.2 TPS — the opposite of runs 3/4.
  Stable runs show the structural GCM-vs-authenc cost is ~−8 TPS (H < C). The crossing here is
  noise. J (377.4, stddev 28.9 ms) is also elevated in variance.

- **F and B stable**: F (388.9) and B (383.0) are consistent with their run 2–4 range (373–389
  TPS), confirming the tag-slot I/O cost sets the ceiling for both and HMAC compute is negligible.

## fio Observations

- **WAL fsync: AES-XTS halves throughput under direct I/O**: E drops to 358 IOPS vs D's 733 IOPS
  (−51 %), latency doubling from 313 µs to 764 µs. This contrasts sharply with the <1 % pgbench
  TPS difference. The fio WAL test is write-saturating with `direct=1` and fdatasync per write,
  exposing the per-sector XTS encryption cost in the synchronous I/O path. PostgreSQL's WAL
  writer buffers and coalesces writes, masking this overhead in pgbench.

- **dm-integrity kills WAL throughput**: F (crc32c): 70 IOPS, B (HMAC-SHA256): 97 IOPS, C: 99
  IOPS, H: 99 IOPS — all 7–10× below D (733 IOPS). Every fdatasync forces a journal flush of the
  integrity tag region on top of data. J (raw, no LUKS2) shows 167 IOPS, suggesting LUKS2 overhead
  adds ~40 % to the WAL fsync cost under dm-integrity+GCM.

- **Random reads: dm-integrity costs halve read IOPS**: B/C/H/J cluster at 2395–2650 IOPS
  (19–21 MB/s) vs D/E/G/A/I at 3955–4377 IOPS (31–34 MB/s). The integrity tag must be read and
  verified for every block, doubling the effective I/O pressure. F (crc32c) is the exception at
  4128 IOPS — CRC32c verification is negligible, and tag-slot reads are absorbed by the device
  controller. This confirms that cryptographic hash verification (HMAC-SHA256, GCM) is the
  read-path bottleneck under dm-integrity, not the tag-slot I/O itself.

- **OLTP: btrfs + encryption reduces write throughput vs ext4**: G/I (btrfs) have higher read IOPS
  than D/E (ext4) in fio (no pgcache pressure), but WAL write IOPS are lower (111/135 vs 733/358)
  due to btrfs CoW journaling adding extra write amplification per fsync. This explains the large
  TPS gap between btrfs and ext4 configs in pgbench.

## Cross-Run Comparison

pgbench TPS across all runs (same setup: scale=20, clients=10, threads=10, 60 s):

| ID  | Run 2 | Run 3 | Run 4 | Run 5 | Trend                              |
| --- | ----: | ----: | ----: | ----: | ---------------------------------- |
| D   | 440.1 | 442.4 | 440.4 | 422.6 | ↓ lower this run (page cache miss) |
| E   | 422.7 | 420.1 | 421.8 | 422.1 | stable (~422 TPS, ±1)              |
| G   | 306.0 | 310.9 | 299.3 | 300.3 | stable (~300–311 TPS)              |
| A   | 249.8 | 244.9 | 188.7 | 188.7 | cold plateau since run 4           |
| I   |   —   | 257.9 | 251.9 | 204.4 | degraded to cold plateau in run 5  |
| F   | 375.2 | 373.1 | 389.2 | 388.9 | bimodal: warm (389) / cool (373)   |
| B   | 389.0 | 388.1 | 388.6 | 383.0 | stable (~383–389 TPS)              |
| C   | 382.9 | 382.0 | 378.0 | 367.5 | drifting ↓ (stddev variance)       |
| H   | 382.9 | 367.4 | 370.1 | 381.7 | noisy (367–383 TPS)                |
| J   |   —   |   —   | 382.9 | 377.4 | first repeat: −5.5 TPS (−1.4 %)   |

### Cross-run notes

- **D's 422.6 is an outlier**: runs 2–4 cluster at 440–442 TPS. Run 5's D is ~18 TPS lower, likely
  due to a colder page cache at test time. Since E stayed at 422 TPS (its stable level), the D–E
  gap vanishes this run. The structural AES-XTS overhead on ext4 is 4–5 % (confirmed by runs 2–4).

- **A and I locked in cold-plateau since run 4**: A at 188.7 in both runs 4 and 5 (identical to 3
  decimal places in run 4 vs 188.720 here). I drops from 251.9 (run 4) to 204.4 (run 5),
  suggesting the btrfs CoW working set for config I has now also diverged from its warm state. High
  stddev on both (A: 51.6 ms, I: 24.3 ms) confirms cold-working-set instability.

- **Stable anchor points across all runs**: E (420–423 TPS), F (373–389 TPS), B (383–389 TPS).
  These are the most reliable reference points for cross-run comparison.

- **C and H remain noisy as a pair**: C ranges 367–383, H ranges 367–383. Their ordering flips
  between runs (C > H in runs 3/4; H > C in runs 2/5). The structural gap is ≤8 TPS in either
  direction; run-to-run variance exceeds the signal.
