#!/bin/sh
# fs_bench.sh -- PostgreSQL-like I/O benchmark: ext4 vs btrfs, plain vs dm-crypt
# Uses dmsetup directly (no cryptsetup).
#
# Tests simulate real PostgreSQL I/O patterns:
#   wal_fsync    -- sequential 8K writes + fdatasync per write (commit latency)
#   wal_group    -- sequential 8K writes + fsync every 16 writes (group commit)
#   seq_scan     -- sequential 8K reads  (full table scan)
#   rand_index   -- random 8K reads      (B-tree index lookup)
#   checkpoint   -- sequential 8K writes, no per-write sync (bgwriter flush)
#   oltp         -- 80% rand read / 20% rand write, 8K (mixed OLTP workload)
#
# All tests use --direct=1 to bypass the page cache and measure raw storage.
# This isolates the storage layer, not the OS buffer cache.
#
# Usage:  sudo bash fs_bench.sh /dev/mmcblk1
# Or:     sudo bash fs_bench.sh /dev/mmcblk1p1
#
# WARNING: DESTROYS ALL DATA on the target device/partition.
# Results written to /tmp/fsbench/

set -eu

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
DEV="${1:?Usage: $0 <device>  e.g.  /dev/mmcblk1}"
MNT="/mnt/fsbench"
OUT="/tmp/fsbench"
MAPPER="fsbench_enc"
KEYFILE="/tmp/fsbench_crypt.key"

# PostgreSQL default page size is 8KB -- all tests use this block size
PG_BS="8k"

# File size per fio job.
# Should be large enough to not fit in RAM to avoid cache effects.
# Tune to ~10-20% of your device.
FIO_SIZE="512M"

# ---------------------------------------------------------------------------
# Sanity checks
# ---------------------------------------------------------------------------
for cmd in fio mkfs.ext4 mkfs.btrfs dmsetup blockdev od; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: '$cmd' not found."
        echo "Install: apt install fio e2fsprogs btrfs-progs dmsetup util-linux"
        exit 1
    fi
done

if [ ! -b "$DEV" ]; then
    echo "ERROR: $DEV is not a block device"
    exit 1
fi

mkdir -p "$MNT" "$OUT"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

log() { echo; echo "===[ $* ]===";}

drop_caches() {
    sync
    echo 3 > /proc/sys/vm/drop_caches
}

run_fio_pg() {
    local tag="$1"
    local dir="$2"

    # ------------------------------------------------------------------
    # WAL fsync
    # PostgreSQL WAL writer: single process, sequential 8K writes,
    # one fdatasync() per transaction commit.
    # Key metric: write IOPS ~ max single-connection TPS ceiling.
    # Key metric: avg + p99 latency ~ commit latency seen by clients.
    # ------------------------------------------------------------------
    log "fio: WAL fsync (commit latency) -- $tag"
    drop_caches
    fio \
        --name=wal_fsync \
        --directory="$dir" \
        --rw=write \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=1 \
        --iodepth=1 \
        --ioengine=psync \
        --direct=1 \
        --fdatasync=1 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_wal_fsync.json"

    # ------------------------------------------------------------------
    # WAL group commit
    # PostgreSQL group commit: multiple transactions share one fsync.
    # fsync=16 simulates ~16 commits batched per flush.
    # Key metric: write IOPS -- should be much higher than wal_fsync.
    # ------------------------------------------------------------------
    log "fio: WAL group commit (fsync/16) -- $tag"
    drop_caches
    fio \
        --name=wal_group \
        --directory="$dir" \
        --rw=write \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=1 \
        --iodepth=1 \
        --ioengine=psync \
        --direct=1 \
        --fsync=16 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_wal_group.json"

    # ------------------------------------------------------------------
    # Sequential scan
    # Full table scan: single process reads large extents of 8K pages.
    # High iodepth simulates PostgreSQL read-ahead (effective_io_concurrency).
    # Key metric: read bandwidth.
    # ------------------------------------------------------------------
    log "fio: seq scan (full table read) -- $tag"
    drop_caches
    fio \
        --name=seq_scan \
        --directory="$dir" \
        --rw=read \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=1 \
        --iodepth=32 \
        --ioengine=libaio \
        --direct=1 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_seq_scan.json"

    # ------------------------------------------------------------------
    # Index scan
    # B-tree traversal: multiple concurrent backends doing random 8K reads.
    # numjobs=4 simulates 4 concurrent connections hitting the index.
    # Key metric: read IOPS.
    # ------------------------------------------------------------------
    log "fio: index scan (random 8K read) -- $tag"
    drop_caches
    fio \
        --name=rand_index \
        --directory="$dir" \
        --rw=randread \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=4 \
        --iodepth=4 \
        --ioengine=libaio \
        --direct=1 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_rand_index.json"

    # ------------------------------------------------------------------
    # Checkpoint / bgwriter
    # PostgreSQL checkpoint: bgwriter flushes dirty 8K pages sequentially.
    # No per-write fsync (single fsync at the end of checkpoint).
    # numjobs=2 simulates bgwriter + checkpointer running together.
    # Key metric: write bandwidth.
    # ------------------------------------------------------------------
    log "fio: checkpoint (bgwriter flush) -- $tag"
    drop_caches
    fio \
        --name=checkpoint \
        --directory="$dir" \
        --rw=write \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=2 \
        --iodepth=16 \
        --ioengine=libaio \
        --direct=1 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_checkpoint.json"

    # ------------------------------------------------------------------
    # OLTP mixed
    # Typical OLTP with warm shared_buffers misses:
    # 80% random reads (cache misses going to disk) +
    # 20% random writes (dirty page eviction / heap updates).
    # numjobs=4 simulates 4 concurrent connections.
    # Key metrics: total IOPS, p99 latency.
    # ------------------------------------------------------------------
    log "fio: OLTP mixed (80r/20w random 8K) -- $tag"
    drop_caches
    fio \
        --name=oltp \
        --directory="$dir" \
        --rw=randrw \
        --rwmixread=80 \
        --bs="$PG_BS" \
        --size="$FIO_SIZE" \
        --numjobs=4 \
        --iodepth=4 \
        --ioengine=libaio \
        --direct=1 \
        --unlink=1 \
        --group_reporting \
        --output-format=json+ \
        --output="$OUT/${tag}_oltp.json"
}

bench_fs() {
    local tag="$1"
    local dev="$2"
    local fs="$3"

    log "Format $dev as $fs  (tag: $tag)"

    if [ "$fs" = "ext4" ]; then
        mkfs.ext4 -F -E lazy_itable_init=0,lazy_journal_init=0 "$dev"
        mount -o noatime,data=writeback "$dev" "$MNT"
    elif [ "$fs" = "btrfs" ]; then
        mkfs.btrfs -f "$dev"
        # nodatacow: disable copy-on-write for database files (same as PostgreSQL recommends)
        # compress=no: raw comparison, no compression
        mount -o noatime,nodatacow,compress=no,ssd "$dev" "$MNT"
    fi

    run_fio_pg "$tag" "$MNT"

    umount "$MNT"
}

open_crypt() {
    dd if=/dev/urandom bs=64 count=1 of="$KEYFILE" status=none
    chmod 400 "$KEYFILE"

    local KEY
    KEY=$(od -An -tx1 "$KEYFILE" | tr -d ' \n')

    local SECTORS
    SECTORS=$(blockdev --getsz "$DEV")

    dmsetup create "$MAPPER" \
        --table "0 $SECTORS crypt aes-xts-plain64 $KEY 0 $DEV 0"

    log "dm-crypt mapper created: /dev/mapper/$MAPPER"
    dmsetup info "$MAPPER"
}

close_crypt() {
    dmsetup remove "$MAPPER"
    rm -f "$KEYFILE"
    log "dm-crypt mapper removed"
}

cleanup() {
    umount "$MNT"            2>/dev/null || true
    dmsetup remove "$MAPPER" 2>/dev/null || true
    rm -f "$KEYFILE"
}
trap cleanup EXIT

# ---------------------------------------------------------------------------
# Phase 1 -- No encryption
# ---------------------------------------------------------------------------
log "PHASE 1: No encryption"

bench_fs "plain_ext4"  "$DEV" "ext4"
bench_fs "plain_btrfs" "$DEV" "btrfs"

# ---------------------------------------------------------------------------
# Phase 2 -- Plain dm-crypt (no LUKS header)
# ---------------------------------------------------------------------------
log "PHASE 2: Plain dm-crypt (aes-xts-plain64, 512-bit key)"

open_crypt

bench_fs "crypt_ext4"  "/dev/mapper/$MAPPER" "ext4"
bench_fs "crypt_btrfs" "/dev/mapper/$MAPPER" "btrfs"

close_crypt

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
log "All benchmarks complete. Results in $OUT"
echo "Parse results with:  python3 fs_bench_summary.py $OUT"
