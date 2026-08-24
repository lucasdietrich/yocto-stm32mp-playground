#!/usr/bin/bash
# Wrapper for bitbake tasks that need an interactive terminal (do_menuconfig,
# do_devshell, ...). The container has no X11/xterm/gnome-terminal etc., so
# OE_TERMINAL="auto" fails with "No valid terminal found". Running bitbake
# inside a tmux session makes bitbake pick the "tmux-running" backend
# (it detects $TMUX and runs "tmux split-window"), opening the interactive
# UI as a new pane in this same terminal.
# See openembedded-core/meta/lib/oe/terminal.py (TmuxRunning).
#
# Usage:
#   ./scripts/menuconfig.sh busybox
#   ./scripts/menuconfig.sh -c devshell virtual/kernel
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_container-run.sh"

bitbake_cmd=""
for arg in "$@"; do
    bitbake_cmd+=" $(printf '%q' "${arg}")"
done

container_run tmux new-session -s menuconfig "bitbake${bitbake_cmd}; bash"
