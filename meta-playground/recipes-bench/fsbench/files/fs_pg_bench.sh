#!/usr/bin/env bash
#
# PostgreSQL storage stack benchmark. No LUKS anywhere.
#
# Mandatory configs (answer the user's question):
#   A  dm-crypt (aes-xts-plain64) + btrfs
#   B  dm-integrity(hmac-sha256, separate key) + dm-crypt(aes-xts-plain64) + ext4
#      Two independent layers. dm-integrity owns HMAC key, dm-crypt owns XTS key.
#      Tag slot: 32 bytes (HMAC output only; XTS IV is the sector number, not stored).
#   C  dm-integrity(tag storage only) + dm-crypt(authenc combined) + ext4
#      dm-crypt owns both XTS and HMAC in one pass. Random per-sector IV persisted.
#      Tag slot: 48 bytes (32 HMAC + 16 random IV).
#
# Suggested additional configs that make the comparison meaningful:
#   D  Raw ext4, no encryption, no integrity -- essential performance ceiling.
#      Every overhead seen in A/B/C is measured relative to this.
#   E  dm-crypt (aes-xts-plain64) + ext4 -- same cipher as A but ext4.
#      Isolates the filesystem choice (btrfs vs ext4) from the encryption cost.
#   F  dm-integrity (crc32c, no encryption) + ext4 -- non-keyed checksum only.
#      Shows the raw I/O overhead of dm-integrity's tag slot writes, with no
#      cryptographic hash cost. If B is much slower than F, HMAC is the bottleneck.
#      If B is close to F, the tag I/O itself is the bottleneck, not the hash.
#   J  integritysetup(aead passthrough) + dmsetup(capi:gcm(aes)-random) + ext4
#      Identical cipher stack to H but with zero LUKS2 involvement:
#        - no 16 MB on-device header
#        - no Argon2 KDF at open time
#        - keyfile used directly as the 256-bit GCM volume key
#      Tag slot: 28 bytes (12-byte random nonce + 16-byte GCM tag), same as H.
#      Delta vs H = pure LUKS2 overhead (header I/O + Argon2 boot cost).
#
# Execution order: D -> E -> A -> F -> B -> C
# This adds one variable at a time and makes each delta readable.
#
# WARNING: DESTRUCTIVE. Wipes DEVICE completely on every config. Root required.
# Requires: cryptsetup (ships integritysetup), btrfs-progs, e2fsprogs,
#           postgresql, pgbench (postgresql-contrib on Debian/Ubuntu), fio
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
DEVICE="/dev/mmcblk1p1"
MOUNTPOINT="/mnt/pgbench"
CRYPT_MAPPER="pgbench_crypt"
INTEGRITY_MAPPER="pgbench_integrity"

# Three separate key files. Plain dm-crypt and integritysetup have no on-disk
# header, so losing these files means losing all data on the device permanently.
CRYPT_KEYFILE="/home/root/pgbench_crypt.key"
#   Config A, B, E: 32 bytes (256 bits for AES-128-XTS).

INTEGRITY_HMAC_KEYFILE="/home/root/pgbench_hmac.key"
#   Config B only: 32 bytes (256-bit HMAC-SHA256 key, owned by dm-integrity).

AUTHENC_KEYFILE="/home/root/pgbench_authenc.key"
#   Config C only: 64 bytes. dm-crypt interprets this as two concatenated keys:
#   first 32 bytes -> AES-128-XTS key, last 32 bytes -> HMAC-SHA256 key.
#   See note in setup_C for how dm-crypt splits the key internally.

# AES-128-XTS key size in bits. Used for configs A, B, E.
# 256 bits = two 128-bit AES keys inside XTS. Matches the XTS portion of
# authenc in config C (also 32 bytes / 256 bits).
XTS_KEYSIZE=256

# dm-integrity journal mode, applied to configs B, C, F.
# "bitmap"    - recommended: no double-write, dirty regions recalculated after crash
# "journal"   - safest: writes everything twice, maximum crash safety
# "nojournal" - fastest: no protection at all against torn sector writes
INTEGRITY_MODE="journal"

PG_DATADIR="${MOUNTPOINT}/pgdata"
PG_PORT=5432
PG_USER="postgres"
PGBENCH_SCALE=20          # ~750MB dataset at scale 50; large enough to exceed RAM
PGBENCH_CLIENTS=10
PGBENCH_DURATION=60      # seconds per measured run
WARMUP_DURATION=10        # seconds discarded before measurement

# fio mini-suite: 3 tests × FIO_RUNTIME s ≈ 1 min per config (plus file-create overhead).
# FIO_SIZE is the working-set file per job; kept small so the create phase is fast.
FIO_RUNTIME=15            # seconds per fio test
FIO_SIZE="64M"            # working-set file size per fio job

RESULTS_DIR="/var/lib/pg_bench_results_$(date +%Y%m%d_%H%M%S)"
PG_LOGDIR="${RESULTS_DIR}/logs"

mkdir -p "$RESULTS_DIR" "$PG_LOGDIR"
chmod 777 "$RESULTS_DIR" "$PG_LOGDIR"

# ---------------------------------------------------------------------------
# Safety checks
# ---------------------------------------------------------------------------
require_root() {
    [ "$(id -u)" -eq 0 ] || { echo "Must run as root." >&2; exit 1; }
}

# ---------------------------------------------------------------------------
# Key generation
# ---------------------------------------------------------------------------
generate_keyfiles() {
    for spec in \
        "$CRYPT_KEYFILE:32" \
        "$INTEGRITY_HMAC_KEYFILE:32" \
        "$AUTHENC_KEYFILE:64"
    do
        local file="${spec%%:*}"
        local size="${spec##*:}"
        if [ ! -f "$file" ]; then
            echo "Generating $file ($size bytes)"
            dd if=/dev/urandom of="$file" bs="$size" count=1 status=none
            chmod 600 "$file"
        fi
    done
}

# ---------------------------------------------------------------------------
# Generic helpers
# ---------------------------------------------------------------------------

# Always close top to bottom: filesystem -> crypt -> integrity.
cleanup_mounts() {
    umount "$MOUNTPOINT" 2>/dev/null || true
    # cryptsetup close handles LUKS2 and plain cryptsetup devices.
    # dmsetup remove is the fallback for config J (mapper opened via dmsetup directly).
    cryptsetup close "$CRYPT_MAPPER" 2>/dev/null || \
        dmsetup remove "$CRYPT_MAPPER" 2>/dev/null || true
    integritysetup close "$INTEGRITY_MAPPER" 2>/dev/null || \
        dmsetup remove "$INTEGRITY_MAPPER" 2>/dev/null || true
}
trap cleanup_mounts EXIT

wipe_device() {
    cleanup_mounts
    wipefs -a "$DEVICE"
    dd if=/dev/zero of="$DEVICE" bs=1M count=16 status=none || true
    udevadm settle
}

# Build integritysetup format and open flags from INTEGRITY_MODE.
# Bitmap flag goes to format; nojournal flag goes to open.
integrity_format_flags() {
    case "$INTEGRITY_MODE" in
        bitmap)    echo "--integrity-bitmap-mode" ;;
        journal)   ;;
        nojournal) ;;
        *) echo "Unknown INTEGRITY_MODE: $INTEGRITY_MODE" >&2; exit 1 ;;
    esac
}

integrity_open_flags() {
    case "$INTEGRITY_MODE" in
        nojournal) echo "--integrity-no-journal" ;;
        *) ;;
    esac
}

drop_caches() {
    sync
    echo 3 > /proc/sys/vm/drop_caches
}

stop_postgres() {
    su -s /bin/sh -l "$PG_USER" -c "pg_ctl -D '$PG_DATADIR' stop -m fast" 2>/dev/null || true
    sleep 2
}

start_postgres() {
    local label="$1"
    su -s /bin/sh -l "$PG_USER" -c \
        "pg_ctl -D '$PG_DATADIR' -o '-p $PG_PORT' -l '$PG_LOGDIR/pg_${label}.log' start -w"
    sleep 2
}

init_postgres() {
    # Chown the mountpoint so postgres can create pgdata inside it.
    # Do not pre-create pgdata: initdb creates it with the right permissions.
    chown "$PG_USER":"$PG_USER" "$MOUNTPOINT"
    su -s /bin/sh -l "$PG_USER" -c "initdb -D '$PG_DATADIR' --auth=trust"
    cat >> "$PG_DATADIR/postgresql.conf" <<EOF
listen_addresses = 'localhost'
port = $PG_PORT
shared_buffers = 256MB
max_wal_size = 2GB
checkpoint_timeout = 10min
synchronous_commit = on
fsync = on
EOF
}

run_pgbench() {
    local label="$1"
    su -s /bin/sh -l "$PG_USER" -c "createdb -p $PG_PORT bench"
    su -s /bin/sh -l "$PG_USER" -c "pgbench -p $PG_PORT -i -s $PGBENCH_SCALE bench"
    echo "Warming up ($WARMUP_DURATION s, discarded)..."
    su -s /bin/sh -l "$PG_USER" -c \
        "pgbench -p $PG_PORT -c $PGBENCH_CLIENTS -T $WARMUP_DURATION bench" > /dev/null
    echo "Benchmarking $label ($PGBENCH_DURATION s)..."
    su -s /bin/sh -l "$PG_USER" -c \
        "pgbench -p $PG_PORT -c $PGBENCH_CLIENTS -j $PGBENCH_CLIENTS \
         -T $PGBENCH_DURATION -P 10 --progress-timestamp bench" \
        | tee "$RESULTS_DIR/pgbench_${label}.txt"
    su -s /bin/sh -l "$PG_USER" -c "dropdb -p $PG_PORT bench"
}

# ---------------------------------------------------------------------------
# fio mini-suite (~1 min)
#
# Three tests that cover the dominant pgbench I/O patterns:
#   wal_fsync   -- sequential 8K write + fdatasync per write
#                  Direct model of the WAL commit path (synchronous_commit=on).
#                  Key metric: write IOPS / p99 latency.
#   rand_index  -- 4-job random 8K read
#                  Models B-tree and heap page lookups (pgbench_accounts).
#                  Key metric: read IOPS.
#   oltp        -- 4-job 80% rand read / 20% rand write, 8K
#                  Models combined heap reads + dirty-page eviction.
#                  Key metric: total IOPS, p99 latency.
#
# --direct=1 on all tests: bypasses page cache, measures raw storage stack.
# --time_based --runtime: fixed wall-clock window, loops over FIO_SIZE if needed.
# --unlink=1: removes fio files after each test (space freed before postgres init).
# drop_caches before each test: ensures cold cache for reads.
# ---------------------------------------------------------------------------
run_fio() {
    local label="$1"
    local dir="$2"

    echo ""
    echo "--- fio: wal_fsync ($FIO_RUNTIME s) -- $label ---"
    drop_caches
    fio \
        --name=wal_fsync \
        --directory="$dir" \
        --rw=write --bs=8k \
        --numjobs=1 --iodepth=1 \
        --ioengine=psync \
        --direct=1 --fdatasync=1 \
        --size="$FIO_SIZE" \
        --runtime="$FIO_RUNTIME" --time_based \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$RESULTS_DIR/fio_${label}_wal_fsync.json"

    echo ""
    echo "--- fio: rand_index ($FIO_RUNTIME s) -- $label ---"
    drop_caches
    fio \
        --name=rand_index \
        --directory="$dir" \
        --rw=randread --bs=8k \
        --numjobs=4 --iodepth=4 \
        --ioengine=libaio \
        --direct=1 \
        --size="$FIO_SIZE" \
        --runtime="$FIO_RUNTIME" --time_based \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$RESULTS_DIR/fio_${label}_rand_index.json"

    echo ""
    echo "--- fio: oltp ($FIO_RUNTIME s) -- $label ---"
    drop_caches
    fio \
        --name=oltp \
        --directory="$dir" \
        --rw=randrw --rwmixread=80 --bs=8k \
        --numjobs=4 --iodepth=4 \
        --ioengine=libaio \
        --direct=1 \
        --size="$FIO_SIZE" \
        --runtime="$FIO_RUNTIME" --time_based \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$RESULTS_DIR/fio_${label}_oltp.json"
}

run_config() {
    local label="$1"
    local setup_fn="$2"
    echo ""
    echo "======================================================================"
    echo "  Config: $label"
    echo "======================================================================"
    "$setup_fn"
    run_fio "$label" "$MOUNTPOINT"
    drop_caches
    init_postgres
    start_postgres "$label"
    run_pgbench "$label"
    stop_postgres
    cleanup_mounts
}

# ---------------------------------------------------------------------------
# Config D: raw ext4 -- performance ceiling baseline
#
# No encryption, no integrity. Measures what the device can do unencumbered.
# Every overhead in A/B/C/E/F is the delta relative to this number.
# ---------------------------------------------------------------------------
setup_D() {
    echo "--- raw ext4 (no encryption, no integrity) ---"
    wipe_device
    mkfs.ext4 -F "$DEVICE"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "$DEVICE" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config G: raw btrfs -- btrfs performance ceiling baseline
#
# Companion to D. Same idea but btrfs, so the delta between D and G is the
# raw cost of btrfs vs ext4 with zero other variables. The delta between G
# and A then isolates the dm-crypt overhead on btrfs specifically.
# ---------------------------------------------------------------------------
setup_G() {
    echo "--- raw btrfs (no encryption, no integrity) ---"
    wipe_device
    mkfs.btrfs -f "$DEVICE"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "$DEVICE" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config E: dm-crypt (aes-xts-plain64) + ext4 -- encryption overhead baseline
#
# Adds only AES-XTS encryption on top of raw ext4. No integrity layer.
# Delta vs D = pure dm-crypt overhead.
# Delta between A and E = btrfs vs ext4 on the same cipher.
# ---------------------------------------------------------------------------
setup_E() {
    echo "--- dm-crypt (aes-xts-plain64) + ext4 ---"
    wipe_device
    cryptsetup open --type plain \
        --cipher aes-xts-plain64 --key-size "$XTS_KEYSIZE" \
        --key-file "$CRYPT_KEYFILE" \
        "$DEVICE" "$CRYPT_MAPPER"
    mkfs.ext4 -F "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config A: dm-crypt (aes-xts-plain64) + btrfs
#
# The original config the user asked for. Same cipher as E but btrfs.
# Delta vs E = btrfs vs ext4 filesystem overhead.
# ---------------------------------------------------------------------------
setup_A() {
    echo "--- dm-crypt (aes-xts-plain64) + btrfs ---"
    wipe_device
    cryptsetup open --type plain \
        --cipher aes-xts-plain64 --key-size "$XTS_KEYSIZE" \
        --key-file "$CRYPT_KEYFILE" \
        "$DEVICE" "$CRYPT_MAPPER"
    mkfs.btrfs -f "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config F: dm-integrity (crc32c, no encryption) + ext4 -- integrity I/O baseline
#
# Adds dm-integrity with a non-cryptographic CRC32c checksum but no encryption.
# No HMAC key needed. This measures the pure I/O cost of dm-integrity's tag
# slot mechanism (reading/writing the per-sector tag area) without any hash cost.
#
# Delta vs D = raw dm-integrity I/O overhead (tag area reads/writes only).
# Delta between B and F = HMAC-SHA256 computation cost on top of tag I/O.
# Delta between C and F = authenc computation + random IV cost on top of tag I/O.
# ---------------------------------------------------------------------------
setup_F() {
    echo "--- dm-integrity (crc32c, no encryption) + ext4 ---"
    wipe_device
    # shellcheck disable=SC2046
    integritysetup --batch-mode format \
        $(integrity_format_flags) \
        --integrity crc32c \
        "$DEVICE"
    # shellcheck disable=SC2046
    integritysetup open \
        $(integrity_open_flags) \
        --integrity crc32c \
        "$DEVICE" "$INTEGRITY_MAPPER"
    mkfs.ext4 -F "/dev/mapper/$INTEGRITY_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$INTEGRITY_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config B: dm-integrity(hmac-sha256) + dm-crypt(aes-xts-plain64) + ext4
#
# Two fully independent layers, each with its own key.
# dm-integrity sits below dm-crypt and computes HMAC-SHA256 over encrypted
# sectors using INTEGRITY_HMAC_KEYFILE. It has no knowledge of dm-crypt above it.
# dm-crypt sits above and does AES-128-XTS using CRYPT_KEYFILE.
#
# XTS uses the sector number as IV (deterministic), so no IV needs to be stored.
# Tag slot: 32 bytes (HMAC-SHA256 output only).
#
# Delta vs F = HMAC-SHA256 computation cost (dm-integrity is active participant).
# Delta vs C = architectural difference: separate keys, no random IV, 32 vs 48 byte tags.
# ---------------------------------------------------------------------------
setup_B() {
    echo "--- dm-integrity(hmac-sha256, separate) + dm-crypt(aes-xts-plain64) + ext4 ---"
    wipe_device
    # shellcheck disable=SC2046
    integritysetup --batch-mode format \
        $(integrity_format_flags) \
        --integrity hmac-sha256 \
        --integrity-key-file "$INTEGRITY_HMAC_KEYFILE" \
        --integrity-key-size 32 \
        "$DEVICE"
    # shellcheck disable=SC2046
    integritysetup open \
        $(integrity_open_flags) \
        --integrity hmac-sha256 \
        --integrity-key-file "$INTEGRITY_HMAC_KEYFILE" \
        --integrity-key-size 32 \
        "$DEVICE" "$INTEGRITY_MAPPER"
    cryptsetup open --type plain \
        --cipher aes-xts-plain64 --key-size "$XTS_KEYSIZE" \
        --key-file "$CRYPT_KEYFILE" \
        "/dev/mapper/$INTEGRITY_MAPPER" "$CRYPT_MAPPER"
    mkfs.ext4 -F "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config C: LUKS2(authenc combined, integrity aead) + ext4
#
# LUKS2 with --cipher aes-xts-plain64 + --integrity hmac-sha256 sets up the
# dm-integrity + dm-crypt(authenc) stack internally. cryptsetup constructs the
# authenc(hmac(sha256),xts(aes)) combination from the two flags; no raw capi:
# cipher string needed.
#
# cryptsetup open --type plain never supported --integrity, so the manual
# integritysetup + plain dm-crypt approach is not viable.
#
# LUKS2 computes a per-sector HMAC-SHA256 tag and persists a random IV
# alongside it (same security properties as the original design):
#   - IND-CPA: random IV means same plaintext -> different ciphertext each write
#   - AES-256-XTS + HMAC-SHA256 in one pass
#   - Tag slot managed automatically by LUKS2
#
# Trade-off vs plain: LUKS2 stores a 16 MB header at the start of the device.
# For benchmarking on a 256 MB partition this is acceptable.
#
# Key file (AUTHENC_KEYFILE, 64 bytes) is used as the LUKS2 passphrase fed
# into Argon2 to derive the volume key.
#
# Delta vs B = LUKS2 overhead, random IV cost, single-pass vs two-pass.
# ---------------------------------------------------------------------------
setup_C() {
    echo "--- LUKS2 authenc(hmac(sha256),xts(aes)) + integrity aead + ext4 ---"
    wipe_device

    # LUKS2 manages dm-integrity + dm-crypt(authenc) as one unit.
    # --cipher aes-xts-plain64 + --integrity hmac-sha256: LUKS2 combines these
    # into authenc(hmac(sha256),xts(aes)) internally.
    # --key-size 512: 512-bit AES-XTS key (two 256-bit AES keys = AES-256-XTS).
    # LUKS2 allocates the HMAC-SHA256 key separately from the volume key.
    cryptsetup luksFormat --type luks2 \
        --batch-mode \
        --cipher aes-xts-plain64 \
        --key-size 512 \
        --integrity hmac-sha256 \
        --key-file "$AUTHENC_KEYFILE" \
        "$DEVICE"

    cryptsetup luksOpen \
        --key-file "$AUTHENC_KEYFILE" \
        "$DEVICE" "$CRYPT_MAPPER"

    mkfs.ext4 -F "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config I: dm-crypt (aes-cbc-plain64, allow_discards, sector_size=4096) + btrfs
#
# Matches a production dmsetup table exactly:
#   0 <sectors> crypt aes-cbc-plain64 :<keylen>:<type>:<name> 0 <dev> 0 2 allow_discards sector_size:4096
#
# Differences from A:
#   - CBC vs XTS: no per-sector tweak in CBC; IV is the sector number (plain64)
#   - sector_size:4096: dm-crypt logical sector set to 4 KiB, reduces per-sector
#     overhead (fewer IV derivations, fewer tag writes if integrity were present)
#   - allow_discards: TRIM/unmap pass-through enabled
#   - no_write_workqueue / submit_from_crypt_cpus: NOT used (production code
#     skips these when "aes" appears in the cipher name, assuming hw acceleration)
#
# Delta vs A = CBC vs XTS + sector_size + discards, all on btrfs.
# Delta vs G = dm-crypt(CBC) overhead on btrfs with production flags.
# ---------------------------------------------------------------------------
setup_I() {
    echo "--- dm-crypt (aes-cbc-plain64, sector_size=4096, allow_discards) + btrfs ---"
    wipe_device
    cryptsetup open --type plain \
        --cipher aes-cbc-plain64 --key-size "$XTS_KEYSIZE" \
        --allow-discards \
        --sector-size 4096 \
        --key-file "$CRYPT_KEYFILE" \
        "$DEVICE" "$CRYPT_MAPPER"
    mkfs.btrfs -f "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config H: LUKS2(aes-gcm-random, native AEAD) + ext4
#
# AES-256-GCM is a native AEAD cipher: encryption (AES-CTR) and authentication
# (GHASH) happen in a single pass with a single key. No authenc construction
# needed. LUKS2 with --integrity aead uses dm-integrity for the tag slot and
# dm-crypt for the GCM AEAD operation.
#
# Key differences vs C (XTS + HMAC-SHA256 via authenc):
#   - Cipher mode: GCM (CTR-based stream) vs XTS (tweaked block cipher)
#   - Single 256-bit key vs two-part 512-bit key
#   - GCM tag: 16 bytes; GHASH often has dedicated hardware on ARM (pmull)
#   - GCM is susceptible to nonce-reuse; LUKS2 random nonce mitigates this
#   - XTS is disk-sector-optimised; GCM is not (no sector-tweak)
#
# Delta vs C = stream cipher vs sector-tweaked cipher, single-key vs split-key,
#              GHASH vs HMAC for authentication.
# ---------------------------------------------------------------------------
setup_H() {
    echo "--- LUKS2 aes-gcm-random + integrity aead + ext4 ---"
    wipe_device

    # --cipher aes-gcm-random: passes the kernel crypto API name directly.
    # aes-gcm-random shorthand is not recognised by this cryptsetup build.
    # --key-size 256: single 256-bit key (GCM does not split the key like XTS).
    # --integrity aead: LUKS2 sets up dm-integrity tag slot for the GCM tag.
    cryptsetup luksFormat --type luks2 \
        --batch-mode \
        --cipher "aes-gcm-random" \
        --key-size 256 \
        --integrity aead \
        --key-file "$AUTHENC_KEYFILE" \
        "$DEVICE"

    cryptsetup luksOpen \
        --key-file "$AUTHENC_KEYFILE" \
        "$DEVICE" "$CRYPT_MAPPER"

    # /dev/mapper/pgbench_crypt is active and is in use.
    #   type:    LUKS2
    #   cipher:  aes-gcm-random
    #   keysize: 256 bits
    #   key location: keyring
    #   integrity: aead
    #   integrity tag size: 28 bytes
    #   device:  /dev/mmcblk1p1
    #   sector size:  512
    #   offset:  0 sectors
    #   size:    3913752 sectors
    #   mode:    read/write

    mkfs.ext4 -F "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}

# ---------------------------------------------------------------------------
# Config J: dm-integrity(aead passthrough) + dm-crypt(aes-gcm-random), no LUKS2
#
# integritysetup used only to write the superblock and read back
# provided_data_sectors. Final activation is raw dmsetup: integritysetup open
# cannot do external-tag/AEAD passthrough mode.
#
# Key differences vs the previous attempts:
#   --integrity crc32c on format: explicit avoids version-dependent defaults.
#     The stored algorithm is irrelevant -- we never call integritysetup open,
#     so no crc32c is ever computed at runtime.
#   mode D, 0 optional params in integrity table: direct writes, no journal.
#   dd zeroing through the crypt mapper: seeds valid GCM tags for every sector
#     before mkfs. Without this, any read of an unwritten sector causes a GCM
#     authentication failure (tag slot contains uninitialized bytes).
#
# Delta vs H = pure LUKS2 overhead (no 16 MB header, no Argon2 at open time,
#              keyfile bytes used directly as the 256-bit volume key).
# ---------------------------------------------------------------------------
setup_J() {
    echo "--- integrity(aead) + crypt(aes-gcm-random) via dmsetup, no LUKS2 ---"
    wipe_device

    integritysetup format "$DEVICE" --batch-mode --no-wipe --tag-size 28 --integrity crc32c

    local PROVIDED_SECTORS
    PROVIDED_SECTORS=$(integritysetup dump "$DEVICE" | awk '/^provided_data_sectors/{print $2}')
    echo "provided_data_sectors: $PROVIDED_SECTORS"

    # Direct-write mode (D), no optional parameters: kernel stores/retrieves the
    # raw 28-byte tag slot without computing any checksum of its own.
    dmsetup create "$INTEGRITY_MAPPER" \
        --table "0 $PROVIDED_SECTORS integrity $DEVICE 0 28 D 0"

    # First 32 bytes of AUTHENC_KEYFILE = 256-bit AES-GCM key, used directly.
    local KEY_HEX
    KEY_HEX=$(od -An -tx1 -N32 "$AUTHENC_KEYFILE" | tr -d ' \n')
    echo "key length (must be 64): $(echo -n "$KEY_HEX" | wc -c)"

    dmsetup create "$CRYPT_MAPPER" \
        --table "0 $PROVIDED_SECTORS crypt capi:gcm(aes)-random $KEY_HEX 0 /dev/mapper/$INTEGRITY_MAPPER 0 1 integrity:28:aead"

    # Seed every sector with a valid GCM tag by writing zeros through the crypt
    # layer. Without this, reads of uninitialised sectors trigger GCM auth
    # failures. conv=fsync flushes all tags before mkfs.
    # "No space left on device" at end-of-device is expected; suppress exit.
    dd if=/dev/zero of="/dev/mapper/$CRYPT_MAPPER" bs=1M status=progress conv=fsync || true

    mkfs.ext4 -F "/dev/mapper/$CRYPT_MAPPER"
    mkdir -p "$MOUNTPOINT"
    mount -o noatime "/dev/mapper/$CRYPT_MAPPER" "$MOUNTPOINT"
}
# ---------------------------------------------------------------------------
require_root
# Only the pg_ctl server log is written by postgres; tee runs as root.
chown "$PG_USER" "$PG_LOGDIR"
generate_keyfiles

echo "Results -> $RESULTS_DIR"
echo "INTEGRITY_MODE = $INTEGRITY_MODE"
echo "XTS_KEYSIZE    = $XTS_KEYSIZE bits"

# Run order is deliberate: each step adds exactly one variable.
#
#   D: plain_ext4        raw ext4                          <- ext4 ceiling, zero overhead
#   G: plain_btrfs       raw btrfs                         <- btrfs ceiling, zero overhead
#   E: xts_ext4          aes-xts-plain64 + ext4            <- delta D->E: dm-crypt cost on ext4
#   A: xts_btrfs         aes-xts-plain64 + btrfs           <- delta G->A: dm-crypt cost on btrfs
#                                                              delta E->A: ext4 vs btrfs under xts
#   I: cbc4k_btrfs       aes-cbc-plain64 + 4k sectors      <- delta G->I: dm-crypt(CBC) cost on btrfs
#                        + allow_discards + btrfs              delta A->I: CBC vs XTS + sector_size
#   F: crc32c_ext4       dm-integrity(crc32c) + ext4       <- delta D->F: dm-integrity tag I/O (no hash)
#   B: hmac_xts_ext4     dm-integrity(hmac-sha256, sep.)   <- delta F->B: HMAC-SHA256 compute cost
#                        + dm-crypt(aes-xts) + ext4
#   C: authenc_ext4      LUKS2 authenc(hmac-sha256+xts)    <- delta B->C: single-pass, random IV, LUKS2
#                        + ext4
#   H: gcm_ext4          LUKS2 aes-gcm-random + ext4       <- delta C->H: GCM vs authenc(XTS+HMAC)
#   J: gcm_raw_ext4      integritysetup+dmsetup GCM, no     <- delta H->J: LUKS2 header/KDF overhead
#                        LUKS2 + ext4

run_config "D_plain_ext4"      setup_D
run_config "G_plain_btrfs"     setup_G
run_config "E_xts_ext4"        setup_E
run_config "A_xts_btrfs"       setup_A
run_config "I_cbc4k_btrfs"     setup_I
run_config "F_crc32c_ext4"     setup_F
run_config "B_hmac_xts_ext4"   setup_B
run_config "C_authenc_ext4"    setup_C
run_config "H_gcm_ext4"        setup_H
run_config "J_gcm_raw_ext4"    setup_J

echo ""
echo "All done. Results in $RESULTS_DIR"
echo ""
echo "TPS summary (higher is better):"
grep -H "tps =" "$RESULTS_DIR"/pgbench_*.txt | sort -t_ -k1,1 || true
