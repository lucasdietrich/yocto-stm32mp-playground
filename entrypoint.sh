#!/bin/bash
# Entrypoint for the Yocto build container.
#
# Assumes the whole project (bitbake, openembedded-core, meta-* layers,
# build-* directories) is bind-mounted at $PROJECT_DIR. bitbake must sit
# directly next to openembedded-core, as it does in this project layout,
# so that openembedded-core/oe-init-build-env can find it automatically.
#
# - Initializes the BitBake build environment in $PROJECT_DIR/$BUILD_DIR
#   (creating a template local.conf/bblayers.conf the first time it does
#   not exist yet, e.g. for a brand-new empty build-* directory).
# - Points DL_DIR and SSTATE_DIR at the mounted downloads/sstate-cache
#   dirs via BB_ENV_PASSTHROUGH_ADDITIONS, without touching local.conf
#   (local.conf's own "?=" defaults are weaker than an env-provided value).
# - Execs whatever command was passed to "podman run" (bitbake, devtool,
#   or an interactive shell), with bitbake/devtool already on PATH.
set -euo pipefail

# podman --userns=keep-id can set HOME to the *host* user's home dir when
# there's no matching /etc/passwd entry for that UID in the image. That
# path doesn't exist in this container (only /home/yocto does), which
# breaks bitbake's PRServer (it tries to create a cache dir under $HOME).
# Force it back to the container user's real home.
export HOME=/home/yocto

PROJECT_DIR="${PROJECT_DIR:-/workdir}"
BUILD_DIR="${BUILD_DIR:-build}"
export DL_DIR="${DL_DIR:-${PROJECT_DIR}/downloads}"
export SSTATE_DIR="${SSTATE_DIR:-${PROJECT_DIR}/sstate-cache}"
export BB_ENV_PASSTHROUGH_ADDITIONS="${BB_ENV_PASSTHROUGH_ADDITIONS:-} DL_DIR SSTATE_DIR"

OE_INIT="${PROJECT_DIR}/openembedded-core/oe-init-build-env"

if [ ! -f "${OE_INIT}" ]; then
    echo "Error: ${OE_INIT} not found." >&2
    echo "Did you bind-mount the project directory to ${PROJECT_DIR}?" >&2
    exit 1
fi

mkdir -p "${DL_DIR}" "${SSTATE_DIR}"

cd "${PROJECT_DIR}"

# oe-init-build-env is meant to be sourced; it sets up PATH, BBPATH, etc.
# and creates the build dir (with a template local.conf/bblayers.conf) if
# it does not already exist, which is the case for a fresh build-* dir.
# It isn't written for "set -u" (it references some variables, like
# BBSERVER, without a default), so relax nounset just for this call.
set +u
source "${OE_INIT}" "${PROJECT_DIR}/${BUILD_DIR}" > /dev/null
set -u

exec "$@"