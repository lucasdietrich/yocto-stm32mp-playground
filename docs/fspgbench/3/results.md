# pgbench results — run 3

**Setup:** scale=20, clients=10, threads=10, duration=60 s, device `/dev/mmcblk1p1`  
**PostgreSQL:** 16.12 — TPC-B (sort of), `synchronous_commit=on`, `fsync=on`

New vs run 2: config **I** (production cipher, was pending in run 2) confirmed.

## Results

| ID  | FS    | Encryption                | Integrity              | Key mgmt | Key size                 | IV mode      | Tag (B) |   TPS | Lat avg (ms) |     vs D |
| --- | ----- | ------------------------- | ---------------------- | -------- | ------------------------ | ------------ | ------- | ----: | -----------: | -------: |
| D   | ext4  | none                      | none                   | —        | —                        | —            | —       | 442.4 |         22.6 | baseline |
| E   | ext4  | aes-xts-plain64           | none                   | plain    | 256 b                    | sector#      | —       | 420.1 |         23.8 |   −5.0 % |
| G   | btrfs | none                      | none                   | —        | —                        | —            | —       | 310.9 |         32.1 |  −29.7 % |
| A   | btrfs | aes-xts-plain64           | none                   | plain    | 256 b                    | sector#      | —       | 244.9 |         40.8 |  −44.6 % |
| I ¹ | btrfs | aes-cbc-plain64           | none                   | plain    | 256 b                    | sector# (4K) | —       | 257.9 |         38.7 |  −41.7 % |
| F   | ext4  | none                      | crc32c (non-keyed)     | plain    | —                        | —            | 4       | 373.1 |         26.8 |  −15.7 % |
| B   | ext4  | aes-xts-plain64           | hmac-sha256 (sep. key) | plain    | 256 b crypt + 256 b HMAC | sector#      | 32      | 388.1 |         25.7 |  −12.3 % |
| C   | ext4  | aes-xts-plain64 + authenc | hmac-sha256 (combined) | LUKS2    | 512 b (split)            | random       | 48      | 382.0 |         26.1 |  −13.6 % |
| H   | ext4  | aes-gcm-random            | AEAD (GCM)             | LUKS2    | 256 b                    | random       | 16      | 367.4 |         27.2 |  −16.9 % |

¹ Production match: `aes-cbc-plain64`, `sector_size=4096`, `allow_discards`, btrfs.

## Key deltas

| Delta | Meaning                                                         | Effect              |
| ----- | --------------------------------------------------------------- | ------------------- |
| D → E | pure dm-crypt (aes-xts) overhead on ext4                        | −5.0 %              |
| D → G | btrfs vs ext4, no security layers                               | −29.7 %             |
| E → A | btrfs vs ext4 under dm-crypt                                    | −41.7 % rel. to E   |
| G → I | dm-crypt (CBC, 4K sector) overhead on btrfs — production cipher | −17.1 % rel. to G   |
| A → I | CBC + 4K sectors vs XTS + 512B sectors on btrfs                 | +5.3 % rel. to A    |
| D → F | dm-integrity tag-slot I/O cost (crc32c, no crypto hash)         | −15.7 %             |
| F → B | + HMAC-SHA256 on top of tag I/O (separate key, det. IV)         | +15.0 TPS (+4.0 %)  |
| B → C | LUKS2 + random IV + single-pass authenc vs two-layer plain      | −6.0 TPS (−1.6 %)   |
| C → H | AES-GCM (GHASH+CTR) vs authenc (HMAC+XTS), same LUKS2          | −14.6 TPS (−3.8 %)  |

## Observations

- **Config I (production) only marginally outperforms A (+5.3 %)**: both ran with a warm
  working set in this run, compressing the A→I gap. CBC + 4K sectors lifts TPS from 244.9 (A)
  to 257.9 (I). The production cipher still sits −41.7 % below the unencrypted ext4 baseline
  (D), confirming that btrfs CoW write amplification is the dominant bottleneck. The larger gap
  seen in run 4 (+33.4 %) was driven by A's anomalously cold working set there (latency stddev
  51 ms), not by a cipher advantage.

- **btrfs CoW is the dominant cost**: btrfs alone costs −29.7 % vs ext4 (G vs D); adding
  dm-crypt (A) pushes this to −44.6 %. Config I (CBC + 4K sectors) trims encryption overhead
  but cannot overcome CoW amplification.

- **dm-crypt overhead on ext4 remains small**: AES-XTS adds only −5.0 % on top of raw ext4
  (E vs D), consistent with hardware AES acceleration being active.

- **F and H show anomalously high latency variance**: F stddev 35.4 ms and H stddev 39.1 ms,
  vs 7–16 ms for all other configs. This inflates both deltas: F's TPS is dragged to 373.1
  (vs ~388–389 in stable runs), and H drops to 367.4 vs 382.9 in run 2 and 370.1 in run 4.
  Both gaps are measurement artifacts, not structural cipher costs.

- **B > F (+15.0 TPS) is a noise artifact**: in run 4 (stable run) B ≈ F (388.6 vs 389.2,
  within 0.7 TPS), confirming that HMAC-SHA256 compute is negligible and the tag-slot journal
  write cost sets the ceiling for both. F's depressed TPS here is entirely attributable to its
  high stddev.

- **C → H gap (−14.6 TPS) is inflated by H's variance**: run 2 shows C ≈ H (both 382.9 TPS);
  run 4 narrows to −7.9 TPS. H's high stddev (39.1 ms) in this run produces an outlier low
  mean. The structural GCM vs authenc difference is ~−8 TPS, not −14.6 TPS.

- **Stable anchor points (D, E, B, C)**: D (442.4), E (420.1), B (388.1), and C (382.0) are
  consistent across runs 2, 3, and 4 within 4 TPS, confirming measurement reproducibility for
  configs that do not exhibit stddev spikes.
