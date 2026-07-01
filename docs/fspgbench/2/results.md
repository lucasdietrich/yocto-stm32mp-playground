# pgbench results — run 2

**Setup:** scale=20, clients=10, threads=10, duration=60 s, device `/dev/mmcblk1p1`  
**PostgreSQL:** 16.12 — TPC-B (sort of), `synchronous_commit=on`, `fsync=on`

## Results

| ID  | FS    | Encryption                | Integrity              | Key mgmt | Key size                 | IV mode | Tag (B) |   TPS | Lat avg (ms) |     vs D |
| --- | ----- | ------------------------- | ---------------------- | -------- | ------------------------ | ------- | ------- | ----: | -----------: | -------: |
| D   | ext4  | none                      | none                   | —        | —                        | —       | —       | 440.1 |         22.7 | baseline |
| E   | ext4  | aes-xts-plain64           | none                   | plain    | 256 b                    | sector# | —       | 422.7 |         23.6 |   −3.9 % |
| G   | btrfs | none                      | none                   | —        | —                        | —       | —       | 306.0 |         32.6 |  −30.5 % |
| A   | btrfs | aes-xts-plain64           | none                   | plain    | 256 b                    | sector# | —       | 249.8 |         40.0 |  −43.2 % |
| F   | ext4  | none                      | crc32c (non-keyed)     | plain    | —                        | —       | 4       | 375.2 |         26.6 |  −14.7 % |
| B   | ext4  | aes-xts-plain64           | hmac-sha256 (sep. key) | plain    | 256 b crypt + 256 b HMAC | sector# | 32      | 389.0 |         25.7 |  −11.6 % |
| C   | ext4  | aes-xts-plain64 + authenc | hmac-sha256 (combined) | LUKS2    | 512 b (split)            | random  | 48      | 382.9 |         26.1 |  −13.0 % |
| H   | ext4  | aes-gcm-random            | AEAD (GCM)             | LUKS2    | 256 b                    | random  | 16      | 382.9 |         26.1 |  −13.0 % |

## Key deltas

| Delta | Meaning                                                    | Effect            |
| ----- | ---------------------------------------------------------- | ----------------- |
| D → E | pure dm-crypt (aes-xts) overhead on ext4                   | −3.9 %            |
| D → G | btrfs vs ext4, no security layers                          | −30.5 %           |
| E → A | btrfs vs ext4 under dm-crypt                               | −41.1 % rel. to E |
| D → F | dm-integrity tag-slot I/O cost (crc32c, no crypto hash)    | −14.7 %           |
| F → B | + HMAC-SHA256 on top of tag I/O (separate key, det. IV)    | +13.8 TPS         |
| B → C | LUKS2 + random IV + single-pass authenc vs two-layer plain | −6.1 TPS          |
| C → H | AES-GCM (GHASH+CTR) vs authenc (HMAC+XTS), same LUKS2      | ≈ 0               |

## Observations

- **btrfs CoW is the dominant cost**: btrfs alone costs −30 % vs ext4 (G vs D),
  and −41 % once dm-crypt is added (A vs E). This dwarfs all cryptographic overheads.
- **dm-crypt overhead is small**: adding AES-XTS on top of raw ext4 costs only −3.9 %
  (E vs D), consistent with hardware AES acceleration being active.
- **dm-integrity tag I/O is the bottleneck, not the hash**: F (crc32c, no crypto)
  is slower than B (HMAC-SHA256 + XTS). The tag-slot journal writes dominate;
  HMAC compute is negligible. B benefits from the crypt layer hiding latency.
- **B ≈ C ≈ H (~383–389 TPS)**: two-layer plain (B) and single-pass LUKS2 AEAD
  (C, H) converge. LUKS2 overhead in C/H is offset by doing one I/O pass instead
  of two. GCM (GHASH) and HMAC-SHA256 perform identically here, consistent with
  ARM `pmull` hardware acceleration for both.
- **Random IV (C, H) adds no measurable cost** vs deterministic sector-number IV (B).

## Config I — production match (pending run)

Config I was added to `fs_pg_bench.sh` to match the production
dmsetup table exactly:

```
0 <sectors> crypt aes-cbc-plain64 :<keylen>:<type>:<name> 0 <dev> 0 2 allow_discards sector_size:4096
```

| Parameter                | Value           | Notes                                   |
| ------------------------ | --------------- | --------------------------------------- |
| Cipher                   | aes-cbc-plain64 | CBC, no per-sector tweak (vs XTS in A)  |
| Key size                 | 256 b           | same as A/E                             |
| `sector_size`            | 4096 B          | reduces IV derivation frequency         |
| `allow_discards`         | yes             | TRIM pass-through                       |
| `no_write_workqueue`     | no              | suppressed when cipher contains `"aes"` |
| `submit_from_crypt_cpus` | no              | suppressed when cipher contains `"aes"` |
| FS                       | btrfs           | same as A/G                             |
| Integrity                | none            | —                                       |

Expected deltas once run:

- **G → I**: raw dm-crypt(CBC) overhead on btrfs with production flags
- **A → I**: isolates CBC vs XTS + `sector_size:4096` + `allow_discards` on btrfs
