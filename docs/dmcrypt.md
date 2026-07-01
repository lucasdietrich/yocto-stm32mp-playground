# dm crypt playground

```
# Partition the device (creates /dev/mmcblk1p1 of 256MB)
parted -s /dev/mmcblk1 mklabel gpt
parted -s /dev/mmcblk1 mkpart primary 1MiB 257MiB
udevadm settle

# Then target the partition instead of the whole device
dd if=/dev/urandom of=pgbench_authenc.key bs=64 count=1 status=none
chmod 600 pgbench_authenc.key
wipefs -a /dev/mmcblk1p1
dd if=/dev/zero of=/dev/mmcblk1p1 bs=1M count=16 status=none
udevadm settle

cryptsetup luksFormat --type luks2 --batch-mode --cipher "capi:authenc(hmac(sha256),xts(aes))-random" --key-size 512 --integrity aead --key-file /root/pgbench_authenc.key /dev/mmcblk1p1
cryptsetup luksOpen --key-file /root/pgbench_authenc.key /dev/mmcblk1p1 pgbench_crypt
mkfs.ext4 -F /dev/mapper/pgbench_crypt
```