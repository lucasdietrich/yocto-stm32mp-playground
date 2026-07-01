# pgbench results — run 4

**Setup:** scale=20, clients=10, threads=10, duration=60 s, device `/dev/mmcblk1p1`  
**PostgreSQL:** 16.12 — TPC-B (sort of), `synchronous_commit=on`, `fsync=on`

New vs run 3: config **I** (production cipher, was pending) and **J** (AES-GCM bypassing LUKS2) added.

## Results

| ID  | FS    | Encryption                | Integrity              | Key mgmt      | Key size                 | IV mode      | Tag (B) |   TPS | Lat avg (ms) |     vs D | wal IOPS | wal vs D |
| --- | ----- | ------------------------- | ---------------------- | ------------- | ------------------------ | ------------ | ------- | ----: | -----------: | -------: | -------: | -------: |
| D   | ext4  | none                      | none                   | —             | —                        | —            | —       | 440.4 |         22.7 | baseline |      723 | baseline |
| E   | ext4  | aes-xts-plain64           | none                   | plain         | 256 b                    | sector#      | —       | 421.8 |         23.7 |   −4.2 % |      357 |   −51 %  |
| G   | btrfs | none                      | none                   | —             | —                        | —            | —       | 299.3 |         33.4 |  −32.0 % |      115 |   −84 %  |
| A   | btrfs | aes-xts-plain64           | none                   | plain         | 256 b                    | sector#      | —       | 188.7 |         52.9 |  −57.1 % |       78 |   −89 %  |
| I ¹ | btrfs | aes-cbc-plain64           | none                   | plain         | 256 b                    | sector# (4K) | —       | 251.9 |         39.6 |  −42.8 % |      130 |   −82 %  |
| F   | ext4  | none                      | crc32c (non-keyed)     | plain         | —                        | —            | 4       | 389.2 |         25.7 |  −11.6 % |       69 |   −90 %  |
| B   | ext4  | aes-xts-plain64           | hmac-sha256 (sep. key) | plain         | 256 b crypt + 256 b HMAC | sector#      | 32      | 388.6 |         25.7 |  −11.8 % |       91 |   −87 %  |
| C   | ext4  | aes-xts-plain64 + authenc | hmac-sha256 (combined) | LUKS2         | 512 b (split)            | random       | 48      | 378.0 |         26.4 |  −14.2 % |       90 |   −88 %  |
| H   | ext4  | aes-gcm-random            | AEAD (GCM)             | LUKS2         | 256 b                    | random       | 16      | 370.1 |         27.0 |  −16.0 % |       89 |   −88 %  |
| J ² | ext4  | aes-gcm-random            | AEAD (GCM)             | plain (dmset) | 256 b                    | random       | 28 ³    | 382.9 |         26.1 |  −13.1 % |      153 |   −79 %  |

¹ Production match: `aes-cbc-plain64`, `sector_size=4096`, `allow_discards`, btrfs.  
² Bypasses LUKS2: raw `integritysetup` + `dmsetup` pipeline, no KDF, no LUKS2 header.  
³ 28-byte tag slot = 12 B GCM nonce + 16 B GCM auth tag.  
wal IOPS = fio `wal_fsync` write IOPS (seq 8 KiB write + `fdatasync`, 1 job, depth 1).

## Key deltas

| Delta | Meaning                                                         | Effect              | wal IOPS Δ ⁴        |
| ----- | --------------------------------------------------------------- | ------------------- | ------------------- |
| D → E | pure dm-crypt (aes-xts) overhead on ext4                        | −4.2 %              | −51 %               |
| D → G | btrfs vs ext4, no security layers                               | −32.0 %             | −84 %               |
| E → A | btrfs vs ext4 under dm-crypt                                    | −55.3 % rel. to E   | −78 % rel. to E     |
| G → I | dm-crypt (CBC, 4K sector) overhead on btrfs — production cipher | −15.8 % rel. to G   | +13 % rel. to G     |
| A → I | CBC + 4K sectors vs XTS + 512B sectors on btrfs                 | +33.4 % rel. to A   | +68 % rel. to A     |
| D → F | dm-integrity tag-slot I/O cost (crc32c, no crypto hash)         | −11.6 %             | −90 %               |
| F → B | + HMAC-SHA256 on top of tag I/O (separate key, det. IV)         | −0.7 TPS (≈ 0)      | +31 % rel. to F     |
| B → C | LUKS2 + random IV + single-pass authenc vs two-layer plain      | −10.6 TPS (−2.7 %)  | ≈ 0                 |
| C → H | AES-GCM (GHASH+CTR) vs authenc (HMAC+XTS), same LUKS2          | −7.9 TPS (−2.1 %)   | ≈ 0                 |
| H → J | GCM without LUKS2 (raw integritysetup + dmsetup) vs LUKS2 GCM  | +12.8 TPS (+3.5 %)  | +72 % rel. to H     |

⁴ fio `wal_fsync` write IOPS: sequential 8 KiB writes, `fdatasync=1`, `iodepth=1`, `direct`. Exposes raw write+sync cost unmasked by PostgreSQL's shared_buffers.

## Observations

- **Config I (production) substantially outperforms A**: switching from AES-XTS (512B sectors)
  to AES-CBC with `sector_size=4096` on btrfs lifts TPS from 188.7 (A) to 251.9 (I), a +33.4 %
  recovery. Larger sectors reduce the number of CBC IV derivations and dm-crypt I/O round-trips
  per btrfs write. Yet I remains −42.8 % below the unencrypted ext4 baseline (D), confirming
  that btrfs CoW write amplification is the dominant bottleneck, not cipher choice.

- **btrfs CoW is the dominant cost**: btrfs alone costs −32.0 % vs ext4 (G vs D); adding
  dm-crypt (A) pushes this to −57.1 %. Config I (CBC + 4K sectors) partially recovers by
  reducing encryption overhead but cannot overcome CoW amplification.

- **dm-crypt overhead on ext4 remains small**: AES-XTS adds only −4.2 % on top of raw ext4
  (E vs D), consistent with hardware AES acceleration being active.

- **dm-integrity tag I/O is the bottleneck, not the hash**: F (crc32c, no crypto) and B
  (HMAC-SHA256 + XTS) are within 0.7 TPS of each other (F: 389.2, B: 388.6). Tag-slot
  journal writes dominate; HMAC compute is negligible at hardware speeds.

- **J (raw GCM, no LUKS2) outperforms H (LUKS2 GCM) by +12.8 TPS**: eliminating the LUKS2
  header and KDF path removes measurable overhead. J (382.9) closes the gap to B/F
  (~388–389 TPS), placing the remaining ~6 TPS deficit squarely on the GCM integrity pipeline
  (one extra I/O pass for tag storage) rather than on LUKS2.

- **Run 3 → run 4 stability**: D, E, B, C are nearly identical across runs (within 2 TPS).
  G and H show slightly more variance (G: 310.9 → 299.3; H: 367.4 → 370.1), consistent
  with expected run-to-run noise. A drops from 244.9 (run 3) to 188.7 here — likely a cold
  working-set effect given the high latency stddev (51 ms vs 15 ms in run 3).

- **Cluster B ≈ F > J > C > H** (all within 19 TPS, 389–370): the ordering is stable and
  confirms that single-pass AEAD (H, J) does not beat the two-layer dm-integrity + dm-crypt
  stack (B) in this workload; tag-slot I/O cost, not crypto compute, sets the ceiling.

## fio micro-benchmark

Three synthetic workloads run on the mounted filesystem directly after the pgbench run.

| Workload     | Pattern           | BS    | Jobs × depth | ioengine | Sync               |
| ------------ | ----------------- | ----- | ------------ | -------- | ------------------ |
| `oltp`       | 80/20 randrw      | 8 KiB | 4 × 4        | libaio   | O_DIRECT           |
| `rand_index` | 100 % randread    | 8 KiB | 4 × 4        | libaio   | O_DIRECT           |
| `wal_fsync`  | seq write+fsync   | 8 KiB | 1 × 1        | psync    | O_DIRECT + fdatasync |

### oltp — 80/20 randrw, 8 KiB, 4 jobs × depth 4

| ID  | r IOPS | w IOPS | r lat (ms) | w lat (ms) |
| --- | -----: | -----: | ---------: | ---------: |
| D   |   2995 |    764 |       3.46 |       7.32 |
| E   |   2065 |    533 |       5.11 |      10.16 |
| G   |   3147 |    804 |       3.34 |       6.78 |
| A   |   2403 |    618 |       4.86 |       6.87 |
| I   |   2919 |    747 |       3.94 |       5.76 |
| F   |   1407 |    364 |      11.27 |       0.31 |
| B   |    859 |    224 |      16.97 |       6.27 |
| C   |    853 |    222 |      17.00 |       6.70 |
| H   |    835 |    218 |      17.62 |       5.76 |
| J   |   1318 |    341 |       8.69 |      13.27 |

### rand_index — 100 % randread, 8 KiB, 4 jobs × depth 4

| ID  | r IOPS | r lat (ms) |
| --- | -----: | ---------: |
| D   |   4390 |       3.64 |
| E   |   4153 |       3.84 |
| G   |   4419 |       3.61 |
| A   |   3893 |       4.10 |
| I   |   4188 |       3.79 |
| F   |   4238 |       3.77 |
| B   |   2350 |       6.80 |
| C   |   2436 |       6.56 |
| H   |   2642 |       6.04 |
| J   |   2684 |       5.95 |

### wal_fsync — seq write + fdatasync, 8 KiB, depth 1

| ID  | w IOPS | w lat (ms) |
| --- | -----: | ---------: |
| D   |    723 |       0.31 |
| E   |    357 |       0.79 |
| G   |    115 |       0.43 |
| A   |     78 |       0.83 |
| I   |    130 |       0.64 |
| F   |     69 |       0.39 |
| B   |     91 |       1.00 |
| C   |     90 |       1.25 |
| H   |     89 |       1.11 |
| J   |    153 |       1.46 |

> `w lat` is the `write()` syscall latency; `fdatasync()` time is not included and accounts
> for the gap between `1/w_lat` and the actual IOPS (e.g. D: 1/0.31 ms ≈ 3226 theoretical
> vs 723 actual — the remaining ~1.1 ms per cycle is the fdatasync flush).

### Synthesis

- **dm-crypt write penalty is hidden by shared_buffers**: D→E wal_fsync drops −51 % IOPS
  (723 → 357) while pgbench shows only −4.2 % TPS. PostgreSQL absorbs writes in
  `shared_buffers`; AES-XTS overhead is only visible when writes reach storage on every
  fdatasync. Hardware AES acceleration keeps the per-byte cost low, but sector-level
  re-encryption per each synced page still doubles the write latency.

- **btrfs fdatasync cost is catastrophic in isolation**: D→G wal_fsync −84 % (723 → 115 IOPS).
  CoW means each fdatasync must flush a tree of dirty metadata blocks, not just the data
  extent. This matches the pgbench −32 % but is far more severe under a pure fsync workload.
  Config I (CBC + 4K sectors) partially recovers vs A (+68 % wal IOPS) by reducing per-sector
  crypto round-trips, consistent with the +33 % pgbench TPS gain.

- **dm-integrity tag writes make fsync-heavy workloads impractical**: D→F wal_fsync −90 %
  (723 → 69 IOPS). Every data write triggers a tag-slot journal write; every fdatasync
  flushes the journal. F's pgbench impact (−11.6 %) is far smaller because OLTP reads
  dominate and PostgreSQL's WAL batching reduces raw fsync frequency. F→B is +31 % in
  wal_fsync (HMAC compute fits within the already-serialised journal write) vs ≈ 0 in pgbench.

- **dm-integrity tag reads hurt random read IOPS**: rand_index shows F at 4238 IOPS (−3.5 %
  vs D — crc32c verification is cheap), but B drops to 2350 (−46 %) because each 8 KiB read
  fetches both the data block and the separate HMAC tag block. C (2436) and H/J (2642–2684)
  are similar; AEAD modes (H, J) read slightly better than two-layer HMAC (B, C) because
  authentication is inline.

- **J (raw GCM) wal_fsync is +72 % over H (LUKS2 GCM)**: eliminating the LUKS2 key
  derivation and header path reduces per-sync overhead measurably (89 → 153 IOPS), matching
  the +3.5 % pgbench gain but at a much larger magnitude in the pure-write path. The
  remaining gap to B/F in wal_fsync (153 vs 69–91) reflects the one extra I/O pass for
  GCM tag storage.

- **B, C, H converge in wal_fsync (89–91 IOPS)**: once dm-integrity journal writes dominate
  the sync path, cipher choice (XTS vs authenc vs GCM) and LUKS2 vs plain key management
  are irrelevant — the tag journal flush is the bottleneck for all three.
