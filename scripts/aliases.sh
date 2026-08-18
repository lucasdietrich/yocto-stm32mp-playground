#!/usr/bin/bash
# Aliases bitbake/devtool/oe-pkgdata-util to their yocto-builder container
# wrappers, so they shadow the real tools in the current shell.
# Meant to be sourced, not executed, e.g. from your shell rc:
#   source /path/to/scripts/aliases.sh
# Works under both bash and zsh.

if [ -n "${ZSH_VERSION:-}" ]; then
    _yocto_aliases_self="${(%):-%x}"
else
    _yocto_aliases_self="${BASH_SOURCE[0]}"
fi
_yocto_aliases_dir="$(cd "$(dirname "${_yocto_aliases_self}")" && pwd)"

alias bitbake="${_yocto_aliases_dir}/bitbake.sh"
alias devtool="${_yocto_aliases_dir}/devtool.sh"
alias oe-pkgdata-util="${_yocto_aliases_dir}/oe-pkgdata-util.sh"

unset _yocto_aliases_self _yocto_aliases_dir
