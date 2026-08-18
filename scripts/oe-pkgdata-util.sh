#!/usr/bin/bash
# Wrapper that runs oe-pkgdata-util through the yocto-builder podman
# container, so it can be used as a drop-in replacement for the real
# "oe-pkgdata-util" binary (e.g. via
# "alias oe-pkgdata-util=/path/to/scripts/oe-pkgdata-util.sh").
#
# Usage:
#   ./scripts/oe-pkgdata-util.sh list-pkgs
#   BUILD_DIR=build-mp1-wrynose ./scripts/oe-pkgdata-util.sh find-path /usr/bin/foo
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_container-run.sh"
container_run oe-pkgdata-util "$@"
