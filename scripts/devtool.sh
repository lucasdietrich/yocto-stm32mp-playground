#!/usr/bin/bash
# Wrapper that runs devtool through the yocto-builder podman container,
# so it can be used as a drop-in replacement for the real "devtool" binary
# (e.g. via "alias devtool=/path/to/scripts/devtool.sh").
#
# Usage:
#   ./scripts/devtool.sh add myrecipe /path/to/source
#   BUILD_DIR=build-mp1-wrynose ./scripts/devtool.sh status
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_container-run.sh"
container_run devtool "$@"
