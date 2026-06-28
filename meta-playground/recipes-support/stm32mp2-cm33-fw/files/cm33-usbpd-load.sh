#!/bin/sh
# Load and start the CM33 USBPD firmware via Linux remoteproc.
# Finds the m33 remoteproc device by name, not by index.

RPROC_DIR="/sys/class/remoteproc"
FW_NAME="USBPD_DRP_UCSI_CM33_NonSecure_stripped.elf"

find_m33_rproc() {
    for dev in "${RPROC_DIR}"/remoteproc*; do
        [ -f "${dev}/name" ] || continue
        name=$(cat "${dev}/name")
        if [ "${name}" = "m33" ]; then
            echo "${dev}"
            return 0
        fi
    done
    echo ""
}

ACTION="${1:-start}"
RPROC=$(find_m33_rproc)

if [ -z "${RPROC}" ]; then
    echo "ERROR: no remoteproc device named 'm33' found" >&2
    exit 1
fi

case "${ACTION}" in
    start)
        state=$(cat "${RPROC}/state")
        if [ "${state}" = "running" ]; then
            echo "CM33 already running, stopping first..."
            echo stop > "${RPROC}/state"
        fi

        if [ "$(cat "${RPROC}/fw_format")" = "TEE" ]; then
            FW_NAME="USBPD_DRP_UCSI_CM33_NonSecure_sign.bin"
        fi

        if [ ! -f "/lib/firmware/${FW_NAME}" ]; then
            echo "ERROR: firmware not found: /lib/firmware/${FW_NAME}" >&2
            exit 1
        fi

        echo "${FW_NAME}" > "${RPROC}/firmware"
        echo start > "${RPROC}/state"
        echo "CM33 started with ${FW_NAME}"
        ;;
    stop)
        state=$(cat "${RPROC}/state")
        if [ "${state}" = "offline" ]; then
            echo "CM33 already offline"
        else
            echo stop > "${RPROC}/state"
            echo "CM33 stopped"
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop}" >&2
        exit 1
        ;;
esac
