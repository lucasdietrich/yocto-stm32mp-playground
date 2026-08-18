#!/usr/bin/bash
# Wrapper that runs bitbake through the yocto-builder podman container,
# so it can be used as a drop-in replacement for the real "bitbake" binary
# (e.g. via "alias bitbake=/path/to/scripts/bitbake.sh").
#
# Usage:
#   ./scripts/bitbake.sh amy-image -k
#   BUILD_DIR=build-mp1-wrynose ./scripts/bitbake.sh core-image-minimal
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_container-run.sh"
container_run bitbake "$@"
