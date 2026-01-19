SUMMARY = "Hello World by Cargo for Rust (>= 1.85.0)"
HOMEPAGE = "https://github.com/lucasdietrich/rust-hello-world"

inherit cargo
inherit cargo-update-recipe-crates

LICENSE = "MIT | Apache-2.0"
LIC_FILES_CHKSUM="file://COPYRIGHT;md5=e6b2207ac3740d2d01141c49208c2147"

SRC_URI = "git://github.com/lucasdietrich/rust-hello-world.git;protocol=https;branch=dependency-workspace"
SRCREV="d94157957be474a9e0b2243ed9bda77aaffe632f"

# ws-hello and ws-world dependencies
SRC_URI += "git://github.com/lucasdietrich/rust-hello-world-workspace.git;protocol=https;nobranch=1;name=rust-hello-world-workspace;destsuffix=rust-hello-world-workspace;workspace=ws-hello:ws-hello,ws-world:ws-world\
            "
SRCREV_FORMAT .= "_rust-hello-world-workspace"
SRCREV_rust-hello-world-workspace = "a63ad47f5a7714d8a1259812bf2e44b821d3cb27"        

require rust-example-crates.inc

S = "${WORKDIR}/git"

BBCLASSEXTEND = "native"


python cargo_common_do_patch_paths() {
    import shutil

    cargo_config = os.path.join(d.getVar("CARGO_HOME"), "config.toml")
    if not os.path.exists(cargo_config):
        return

    src_uri = (d.getVar('SRC_URI') or "").split()
    if len(src_uri) == 0:
        return

    patches = dict()
    workdir = d.getVar('WORKDIR')
    fetcher = bb.fetch2.Fetch(src_uri, d)
    for url in fetcher.urls:
        ud = fetcher.ud[url]
        if ud.type == 'git' or ud.type == 'gitsm':
            name = ud.parm.get('name')
            destsuffix = ud.parm.get('destsuffix')
            workspace = ud.parm.get('workspace')
            if name is not None and destsuffix is not None:
                if ud.user:
                    repo = '%s://%s@%s%s' % (ud.proto, ud.user, ud.host, ud.path)
                else:
                    repo = '%s://%s%s' % (ud.proto, ud.host, ud.path)
                if workspace:
                    patches.setdefault(repo, [])
                    for crate in workspace.split(','):
                        crate_name, crate_subpath = crate.split(':')
                        path = '%s = { path = "%s" }' % (crate_name, os.path.join(workdir, destsuffix, crate_subpath))
                        patches[repo].append(path)

    with open(cargo_config, "a+") as config:
        for k, v in patches.items():
            print('\n[patch."%s"]' % k, file=config)
            for name in v:
                print(name, file=config)

    if not patches:
        return

    # Cargo.lock file is needed for to be sure that artifacts
    # downloaded by the fetch steps are those expected by the
    # project and that the possible patches are correctly applied.
    # Moreover since we do not want any modification
    # of this file (for reproducibility purpose), we prevent it by
    # using --frozen flag (in CARGO_BUILD_FLAGS) and raise a clear error
    # here is better than letting cargo tell (in case the file is missing)
    # "Cargo.lock should be modified but --frozen was given"

    lockfile = d.getVar("CARGO_LOCK_PATH")
    if not os.path.exists(lockfile):
        bb.fatal(f"{lockfile} file doesn't exist")

    # There are patched files and so Cargo.lock should be modified but we use
    # --frozen so let's handle that modifications here.
    #
    # Note that a "better" (more elegant ?) would have been to use cargo update for
    # patched packages:
    #  cargo update --offline -p package_1 -p package_2
    # But this is not possible since it requires that cargo local git db
    # to be populated and this is not the case as we fetch git repo ourself.

    lockfile_orig = lockfile + ".orig"
    if not os.path.exists(lockfile_orig):
        shutil.copy(lockfile, lockfile_orig)

    newlines = []
    with open(lockfile_orig, "r") as f:
        for line in f.readlines():
            if not line.startswith("source = \"git"):
                newlines.append(line)

    with open(lockfile, "w") as f:
        f.writelines(newlines)
}