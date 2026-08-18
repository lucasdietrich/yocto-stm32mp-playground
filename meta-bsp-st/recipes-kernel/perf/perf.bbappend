# This kernel's tools/perf/Makefile.config gates the BPF skeleton build with
# `ifdef BUILD_BPF_SKEL`, which checks definedness, not value. perf.bb's
# PACKAGECONFIG[bpf-skel] passes "BUILD_BPF_SKEL=0" when the feature is
# disabled, which still defines the variable and forces a mandatory
# clang-bpf-co-re check, failing the build when no compatible clang is
# present. Clear the flag so the variable is never passed when disabled.
PACKAGECONFIG[bpf-skel] = ""

# PERF_SRC is copied fresh from the kernel tree into ${S} by the
# copy_perf_source_from_kernel do_configure prefunc, so patch it here
# instead of via a normal SRC_URI patch (which would be overwritten).
do_configure:prepend() {
    if [ -e "${S}/tools/lib/bpf/libbpf.c" ]; then
        # The sysroot's glibc returns a const-qualified pointer from
        # strchr() on a const argument; tools/lib/bpf/Makefile builds with
        # -Werror, so this discarded-qualifiers warning is otherwise fatal.
        sed -i "s@next_path = strchr(s, ':');@next_path = (char *)strchr(s, ':');@" \
            ${S}/tools/lib/bpf/libbpf.c
    fi
}