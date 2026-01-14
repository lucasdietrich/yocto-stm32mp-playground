inherit cargo
inherit cargo-update-recipe-crates

SRC_URI = "git://github.com/lucasdietrich/rust-hello-world.git;protocol=https;branch=with-1.85.0-dep"
SRCREV="38baae60bb485ae3a2eaf46cb571733c738ff80a"
LIC_FILES_CHKSUM="file://COPYRIGHT;md5=e6b2207ac3740d2d01141c49208c2147"

require rust-example-crates.inc

SUMMARY = "Hello World by Cargo for Rust (>= 1.85.0)"
HOMEPAGE = "https://github.com/lucasdietrich/rust-hello-world"
LICENSE = "MIT | Apache-2.0"

S = "${WORKDIR}/git"

BBCLASSEXTEND = "native"