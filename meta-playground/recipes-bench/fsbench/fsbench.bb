SUMMARY = "fs_bench is a tool to benchmark filesystem performance"
LICENSE = "CLOSED"

SRC_URI = " \
    file://fs_bench.sh \
    file://fs_bench_summary.py \
"

RDEPENDS:${PN} = " \
    fio \
    e2fsprogs-mke2fs \
    btrfs-tools \
    lvm2 \
    util-linux \
    coreutils \
    python3-core \
    python3-json \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/fs_bench.sh         ${D}${bindir}/fs_bench
    install -m 0755 ${WORKDIR}/fs_bench_summary.py ${D}${bindir}/fs_bench_summary
}

FILES:${PN} = " \
    ${bindir}/fs_bench \
    ${bindir}/fs_bench_summary \
"