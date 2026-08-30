## Build and run container base image:

Build the base image with:

```bash
bitbake example-container-image
```

Run:

```bash
crun --root=. --rootless=1 --log-level=debug run -b /containers/oci-bundle cnt1
```

Get state

```bash
crun --root=/containers/oci-bundle --rootless=1 --log-level=debug state cnt1
```

Result:

```bash
root@mp2:~/rt# crun --root=/containers/oci-bundle --rootless=1 --log-level=debug state cnt1
2018-03-09T17:04:04.637203Z: Using debug verbosity
2018-03-09T17:04:04.637820Z: Loading container from config file: `/containers/oci-bundle/cnt1/config.json`
{
    "ociVersion": "1.0.0",
    "id": "cnt1",
    "pid": 677,
    "status": "running",
    "bundle": "/containers/oci-bundle",
    "rootfs": "rootfs",
    "created": "2018-03-09T15:43:27.469256Z",
    "systemd-scope": "",
    "owner": "root",
    "annotations": {
        "org.opencontainers.image.architecture": "arm64",
        "org.opencontainers.image.author": "oe.patch@oe",
        "org.opencontainers.image.created": "2026-08-30T09:06:22Z",
        "org.opencontainers.image.os": "linux",
        "org.opencontainers.image.ref.name": "wrynose",
        "org.opencontainers.image.revision": "4f9ae75"
    }
}
```

kill the container:

```bash
crun --root=/containers/oci-bundle --rootless=1 --log-level=debug kill cnt1 SIGKILL
```

---

## Legacy

Run 

```bash
mkdir -p oci-bundle
umoci unpack --rootless --image ../build/tmp/deploy/images/qemuarm/example-container-image-latest-oci:latest oci-bundle
fakeroot sh -c "mkfs.erofs -zlz4hc --all-root oci-bundle.img oci-bundle"
scp oci-bundle.img root@192.168.7.2:/home/root
ssh root@192.168.7.2
mkdir -p ~/oci-bundle
mount -o loop oci-bundle.img /home/root/oci-bundle
mkdir -p /tmp/{upper,work,merged}
mount -t overlay overlay -o lowerdir=/home/root/oci-bundle/rootfs,upperdir=/tmp/upper,workdir=/tmp/work /tmp/merged
touch /tmp/merged/etc/resolv.conf
mkdir ~/bundle-w
cd ~/bundle-w
cp ~/oci-bundle/config.json .
ln -s /tmp/merged rootfs
# update config.json
# - add CAP_NET_RAW to the 5 capabilitiess
# - change hostID to 0 in uidMappings
# - change hostID to 0 in gidMappings
# - change noNewPrivileges to false (???)
crun --root=. --rootless=1 --log-level=debug run cnt1
```
Commands:

- crun --root=. --rootless=1 --log-level=debug run -b oci-bundle cnt1
- crun --root=. --rootless=1 --log-level=debug exec --cap=CAP_NET_RAW cnt1 /bin/sh


Gives:

```
/bin/sh: can't access tty; job control turned off
/ # 
/ # /bin/main 
cap_set_proc: Operation not permitted
Process ID: 3
User: root (UID: 0)
Group: root (GID: 0)
Capabilities: cap_net_raw=ep
socket: Operation not permitted
Failed to create a raw socket
```


### `CAP_NET_RAW` issue:

- <https://lwn.net/Articles/978846/>
- <https://man7.org/linux/man-pages/man7/user_namespaces.7.html>

### Work with NFS:

1. Extract with: `runqemu-extract-sdk tmp/deploy/images/qemuarm/image-sandbox-qemuarm.rootfs.tar.bz2 temp-nfs`
2. Run qemu with it: `runqemu qemuarm nographic ./temp-nfs`

## TODOs

- [x] Todo add `cgroup               /sys/fs/cgroup       cgroup2    defaults              0  0` to `/etc/fstab`
- [x] Custom linux which supports `erofs` or change erofs to other filesystem
- [ ] Find a way to not generate the qemuboot.conf file for the example-container-image image (makes no sense), otherwise call runqemu with parameters.

## Build CRUN script

```
#!/bin/bash
set -e

source /opt/lux/1.0/environment-setup-cortexa15t2hf-neon-oe-linux-gnueabi

mkdir -p m4
autoreconf -fi

mkdir -p build
cd build
../configure \
    --host=arm-oe-linux-gnueabi \
    --prefix=/usr \
    --disable-embedded-yajl \
    --enable-caps \
    --enable-seccomp \
    --disable-systemd

make -j$(nproc)
```

## Container

- Explain how to manipulate the base-image <meta-virtualization/classes/image-oci.bbclass>

## Podman

podman run -p 8080:80 -v $(pwd):/www -d docker.io/library/busybox httpd -f -p 80 -h /www
