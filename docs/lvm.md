# LVM + partclone + zstd on /dev/mmcblk1 — Session Notes

**Host:** `root@mp2` (stm32mp257f-dk)
**Device:** `/dev/mmcblk1` — 7.3G eMMC

---

## 1. Setup

### Physical volume and volume group

```
# lsblk /dev/mmcblk1
NAME    MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
mmcblk1 179:0    0  7.3G  0 disk

# pvcreate /dev/mmcblk1
  Physical volume "/dev/mmcblk1" successfully created.

# pvdisplay
  PV Name    /dev/mmcblk1
  VG Name    (empty)
  PV Size    7.28 GiB / not usable 4.00 MiB
  PV UUID    TtLSF4-ttcY-pqwz-xGvR-n92u-5aDs-74lFzl

# vgcreate myvg /dev/mmcblk1
  Volume group "myvg" successfully created
```

### Logical volume + ext4

```
# lvcreate -L 1G -n mydata myvg
  Logical volume "mydata" created.

# mkfs.ext4 /dev/myvg/mydata
  Creating filesystem with 262144 4k blocks and 65536 inodes
  Filesystem UUID: 66e34910-320e-42f1-88cf-0435c0185cdf

# mkdir /mnt/mydata
# mount /dev/myvg/mydata /mnt/mydata

# df -h /mnt/mydata
/dev/mapper/myvg-mydata  974M  280K  906M   1% /mnt/mydata
```

---

## 2. First attempt — FAILED (lesson learned)

Test data written, then `fsfreeze` used before snapshot:

```
# dd if=/dev/zero of=/mnt/mydata/file.txt bs=1M count=100
  104857600 bytes copied, 0.371165 s, 283 MB/s

# fsfreeze -f /mnt/mydata
# lvcreate -L 1G -s -n mydata-snap /dev/myvg/mydata
  device-mapper: suspend ioctl on (252:0) failed: Device or resource busy
  Unable to suspend myvg-mydata (252:0)
  Failed to suspend logical volume myvg/mydata.
  Device myvg-mydata-real (252:1) is used by another device.
  Aborting. Manual intervention required.
```

**Root cause:** `fsfreeze -f` holds a VFS lock. `lvcreate -s` issues a dm-suspend ioctl on the
same device. The two conflict — do not combine them.

### Cleanup of broken snapshot state

```
# dmsetup ls
myvg-mydata          (252:0)
myvg-mydata--snap    (252:3)
myvg-mydata--snap-cow (252:2)
myvg-mydata-real     (252:1)

# lvremove -f /dev/myvg/mydata-snap
  Logical volume "mydata-snap" successfully removed.

# dmsetup remove myvg-mydata-real
  (ok)

# dmsetup remove myvg-mydata--snap-cow
  device-mapper: remove ioctl on myvg-mydata--snap-cow failed: No such device or address
  (benign -- lvremove already cleaned it up)

# dmsetup ls
myvg-mydata (252:0)   <- clean
```

---

## 3. Full teardown and clean restart

```
# vgremove myvg       # also removes mydata LV
  Volume group "myvg" successfully removed

# pvcreate /dev/mmcblk1
# vgcreate myvg /dev/mmcblk1
# lvcreate -L 1G -n mydata myvg
  WARNING: ext4 signature detected on /dev/myvg/mydata at offset 1080. Wipe it? [y/n]: y
  Logical volume "mydata" created.

# mkfs.ext4 /dev/myvg/mydata
# mount /dev/myvg/mydata /mnt/mydata
```

---

## 4. Backup 1 — zeros (highly compressible)

```
# dd if=/dev/zero of=/mnt/mydata/file.txt bs=1M count=100
  104857600 bytes, 283 MB/s

# lvcreate -L 1G -s -n mydata-snap /dev/myvg/mydata
  Logical volume "mydata-snap" created.

# lvs -o name,snap_percent myvg
  LV          Snap%
  mydata
  mydata-snap 0.01

# mkdir -p /backup
# partclone.ext4 -c -s /dev/myvg/mydata-snap -O - | zstd -T0 -9 -o /backup/mydata.ext4.zst
  Device size:    1.1 GB = 262144 Blocks
  Space in use: 158.2 MB = 38619 Blocks
  Total Time: 00:00:04, Ave. Rate: 2.37GB/min, 100.00% completed!

# lvremove -f /dev/myvg/mydata-snap

# ls -lh /backup/mydata.ext4.zst
  -rw-r--r-- 1 root root 11K    <- zeros compress to almost nothing
```

---

## 5. Backup 2 — random data (incompressible)

```
# dd if=/dev/random of=/mnt/mydata/file2.txt bs=1M count=100
  104857600 bytes, 55.9 MB/s

# lvcreate -L 1G -s -n mydata-snap /dev/myvg/mydata

# partclone.ext4 -c -s /dev/myvg/mydata-snap -O - | zstd -T0 -9 -o /backup/mydata2.ext4.zst
  zstd: error 70 : Write error : No space left on device   <- /root only had ~25MB free
```

Redirected to `/mnt/userfs` (14G, `/dev/mmcblk0p9`):

```
# rm -rf /backup/mydata2.ext4.zst
# partclone.ext4 -c -s /dev/myvg/mydata-snap -O - | zstd -T0 -9 -o /mnt/userfs/mydata2.ext4.zst
  Space in use: 263.0 MB = 64219 Blocks
  Total Time: 00:00:06, Ave. Rate: 2.63GB/min, 100.00% completed!
  /*stdin*\: 39.86%  (251 MiB => 100 MiB)   <- random data: ~40% compression ratio

# mv /backup/mydata.ext4.zst /mnt/userfs/

# ls -lh /mnt/userfs/
  11K  mydata.ext4.zst   (zeros,  158 MB source -> 11 KB)
  101M mydata2.ext4.zst  (random, 263 MB source -> 101 MB)
```

### Integrity check

```
# zstd -t /mnt/userfs/mydata.ext4.zst
  /mnt/userfs/mydata.ext4.zst: 158216910 bytes

# zstd -t /mnt/userfs/mydata2.ext4.zst
  /mnt/userfs/mydata2.ext4.zst: 263074910 bytes
```

---

## 6. Restore 1 — mydata.ext4.zst (file.txt only)

```
# lvcreate -L 1G -n mydata-restore myvg

# zstd -d -c /mnt/userfs/mydata.ext4.zst | partclone.restore -s - -O /dev/myvg/mydata-restore
  Space in use: 158.2 MB, Total Time: 00:00:04, 100.00% completed!

# e2fsck -fy /dev/myvg/mydata-restore
  /dev/myvg/mydata-restore: 13/65536 files (7.7% non-contiguous), 38619/262144 blocks

# mkdir -p /mnt/mydata-restore
# mount /dev/myvg/mydata-restore /mnt/mydata-restore

# ls /mnt/mydata-restore
  file.txt  lost+found

# sha256sum /mnt/mydata/file.txt /mnt/mydata-restore/file.txt
  20492a4d0d84f8beb1767f6616229f85d44c2827b64bdbfb260ee12fa1109e0e  /mnt/mydata/file.txt
  20492a4d0d84f8beb1767f6616229f85d44c2827b64bdbfb260ee12fa1109e0e  /mnt/mydata-restore/file.txt
  MATCH
```

---

## 7. Restore 2 — mydata2.ext4.zst (file.txt + file2.txt)

```
# lvcreate -L 1G -n mydata-restore2 myvg

# zstd -d -c /mnt/userfs/mydata2.ext4.zst | partclone.restore -s - -O /dev/myvg/mydata-restore2
  Space in use: 263.0 MB, Total Time: 00:00:04, Ave. Rate: 3.95GB/min, 100.00% completed!

# e2fsck -fy /dev/myvg/mydata-restore2
  /dev/myvg/mydata-restore2: 14/65536 files (7.1% non-contiguous), 64219/262144 blocks

# mkdir -p /mnt/mydata-restore2
# mount /dev/myvg/mydata-restore2 /mnt/mydata-restore2

# sha256sum /mnt/mydata/* /mnt/mydata-restore2/*
  20492a4d0d84f8beb1767f6616229f85d44c2827b64bdbfb260ee12fa1109e0e  /mnt/mydata/file.txt
  8670ac1182dc918c6eb9249cd4d8af04ae250d4263ef853fab6122661dad0422  /mnt/mydata/file2.txt
  20492a4d0d84f8beb1767f6616229f85d44c2827b64bdbfb260ee12fa1109e0e  /mnt/mydata-restore2/file.txt
  8670ac1182dc918c6eb9249cd4d8af04ae250d4263ef853fab6122661dad0422  /mnt/mydata-restore2/file2.txt
  ALL MATCH
```

---

## 8. Key lessons

| Topic                      | Finding                                                                                               |
| -------------------------- | ----------------------------------------------------------------------------------------------------- |
| `fsfreeze` + `lvcreate -s` | Fatal conflict — never combine. LVM snapshot is already atomic without it.                            |
| Broken snapshot cleanup    | `lvremove` is sufficient; manual `dmsetup remove` usually not needed.                                 |
| partclone target           | Always point at the snapshot LV, not the live LV.                                                     |
| Backup destination space   | Zeros compress to near zero; random data at ~40%. Plan disk space accordingly. `/root` was too small. |
| Restore requirements       | Target LV >= source LV size, unmounted, no mkfs beforehand, run `e2fsck -f` after restore.            |
