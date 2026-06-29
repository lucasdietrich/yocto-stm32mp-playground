#!/usr/bin/env python3
# fs_bench_summary.py -- parse fio JSON results from fs_bench.sh (PostgreSQL mode)
# Usage: python3 fs_bench_summary.py /tmp/fsbench

import json
import sys
from pathlib import Path

TAGS = [
    ("plain_ext4",  "Plain   | ext4 "),
    ("plain_btrfs", "Plain   | btrfs"),
    ("crypt_ext4",  "dmsetup | ext4 "),
    ("crypt_btrfs", "dmsetup | btrfs"),
]

# (file_suffix, display_label, rw_direction, description)
TESTS = [
    ("wal_fsync",  "WAL fsync   ", "write",    "write IOPS ~ max TPS; latency ~ commit time"),
    ("wal_group",  "WAL grp-cmt ", "write",    "group commit; IOPS should be >> wal_fsync"),
    ("seq_scan",   "Seq scan    ", "read",     "full table scan; bandwidth matters"),
    ("rand_index", "Index scan  ", "randread", "B-tree lookup; random read IOPS matters"),
    ("checkpoint", "Checkpoint  ", "write",    "bgwriter flush; write bandwidth matters"),
    ("oltp",       "OLTP 80r/20w", "mixed",    "mixed workload; total IOPS + p99 latency"),
]


def load_json(path):
    with open(path) as f:
        return json.load(f)


def job0(data):
    jobs = data.get("jobs", [])
    return jobs[0] if jobs else None


def get_bw_kib(data, rw):
    j = job0(data)
    if j is None:
        return None
    if rw in ("read", "randread"):
        return j["read"]["bw"]
    if rw in ("write", "randwrite"):
        return j["write"]["bw"]
    # mixed: sum both
    return j["read"]["bw"] + j["write"]["bw"]


def get_iops(data, rw):
    j = job0(data)
    if j is None:
        return None
    if rw in ("read", "randread"):
        return j["read"]["iops"]
    if rw in ("write", "randwrite"):
        return j["write"]["iops"]
    return j["read"]["iops"] + j["write"]["iops"]


def get_lat_avg_us(data, rw):
    j = job0(data)
    if j is None:
        return None
    key = "read" if rw in ("read", "randread") else "write"
    lat_ns = j[key].get("lat_ns", {})
    if lat_ns:
        return lat_ns.get("mean", 0) / 1000.0
    return j[key].get("lat", {}).get("mean", 0)


def get_lat_p99_us(data, rw):
    j = job0(data)
    if j is None:
        return None
    key = "read" if rw in ("read", "randread") else "write"
    clat = j[key].get("clat_ns", {})
    if not clat:
        return None
    percentiles = clat.get("percentile", {})
    # fio stores percentile keys as strings with 6 decimal places
    p99 = percentiles.get("99.000000") or percentiles.get("99.0")
    if p99 is not None:
        return p99 / 1000.0
    return None


def fmt_bw(kib):
    if kib is None:
        return "N/A"
    if kib >= 1024 * 1024:
        return f"{kib / 1024 / 1024:.2f} GB/s"
    if kib >= 1024:
        return f"{kib / 1024:.1f} MB/s"
    return f"{kib:.0f} KB/s"


def fmt_iops(iops):
    if iops is None:
        return "N/A"
    if iops >= 1_000_000:
        return f"{iops / 1_000_000:.2f}M"
    if iops >= 1000:
        return f"{iops / 1000:.1f}k"
    return f"{iops:.0f}"


def fmt_lat(us):
    if us is None:
        return "N/A"
    if us >= 1000:
        return f"{us / 1000:.2f}ms"
    return f"{us:.1f}us"


def load_result(base, tag, suffix):
    p = base / f"{tag}_{suffix}.json"
    if not p.exists():
        return None
    try:
        return load_json(p)
    except Exception:
        return None


def print_table(title, description, rows, columns, col_widths):
    """Print one benchmark result table."""
    total = sum(col_widths) + len(col_widths) * 3 + 1
    print()
    print(f"  {title}  --  {description}")
    sep = "+" + "+".join("-" * (w + 2) for w in col_widths) + "+"
    print(sep)
    hdr = "|" + "|".join(f" {c:<{w}} " for c, w in zip(columns, col_widths)) + "|"
    print(hdr)
    print(sep)
    for row in rows:
        line = "|" + "|".join(f" {v:>{w}} " for v, w in zip(row, col_widths)) + "|"
        print(line)
    print(sep)


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <results_dir>")
        sys.exit(1)

    base = Path(sys.argv[1])
    if not base.is_dir():
        print(f"ERROR: {base} is not a directory")
        sys.exit(1)

    columns = ["Config", "Bandwidth", "IOPS", "Avg lat", "p99 lat"]
    col_widths = [18, 12, 10, 10, 10]

    for suffix, label, rw, description in TESTS:
        rows = []
        for tag, config_label in TAGS:
            data = load_result(base, tag, suffix)
            if data is None:
                rows.append([config_label, "N/A", "N/A", "N/A", "N/A"])
                continue
            rows.append([
                config_label,
                fmt_bw(get_bw_kib(data, rw)),
                fmt_iops(get_iops(data, rw)),
                fmt_lat(get_lat_avg_us(data, rw)),
                fmt_lat(get_lat_p99_us(data, rw)),
            ])
        print_table(label, description, rows, columns, col_widths)

    print()
    print("Metrics:")
    print("  Bandwidth : higher is better  (seq scan, checkpoint)")
    print("  IOPS      : higher is better  (WAL = approx max TPS; index = lookup throughput)")
    print("  Avg lat   : lower is better   (commit round-trip time)")
    print("  p99 lat   : lower is better   (tail latency seen by slow clients)")
    print()
    print("PostgreSQL tuning notes:")
    print("  WAL fsync IOPS < 200  --> storage is your TPS bottleneck")
    print("  WAL group >> WAL fsync --> synchronous_commit=off would help greatly")
    print("  btrfs nodatacow used  --> avoids COW write amplification on heap files")
    print("  dm-crypt overhead     --> run 'cryptsetup benchmark' to check AES-NI/CE")


if __name__ == "__main__":
    main()
