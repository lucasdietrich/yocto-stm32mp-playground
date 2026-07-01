#!/bin/bash

set -euo pipefail

SCRIPT_NAME=$(basename "$0")

usage() {
    cat <<EOF
Usage: $SCRIPT_NAME <machine> [options]

Arguments:
  machine               Target machine (e.g., dk2, mp2)

Options:
  -i, --image IMAGE     Image name (default: amy-image)
  -H, --host HOST       IP address or hostname (default: 10.77.0.1)
  -p, --port PORT       Port (default: 8087)
  -b, --build-dir DIR   Build directory (auto-detected if omitted)
  -h, --help            Show this help message

Examples:
  $SCRIPT_NAME dk2
  $SCRIPT_NAME mp2 --host 10.77.0.1
  $SCRIPT_NAME dk2 --image my-image --port 8080
  $SCRIPT_NAME mp2 --host 192.168.10.100 --build-dir build-custom
EOF
}

# Defaults
image="amy-image"
host="10.77.0.1"
port="8087"
build_dir=""

if [[ $# -eq 0 ]]; then
    echo "Error: machine argument is required" >&2
    echo >&2
    usage >&2
    exit 1
fi

machine="$1"
shift

while [[ $# -gt 0 ]]; do
    case "$1" in
        -i|--image)
            image="$2"
            shift 2
            ;;
        -H|--host)
            host="$2"
            shift 2
            ;;
        -p|--port)
            port="$2"
            shift 2
            ;;
        -b|--build-dir)
            build_dir="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Error: unknown option '$1'" >&2
            echo >&2
            usage >&2
            exit 1
            ;;
    esac
done

# Auto-detect build directory from machine name
if [[ -z "$build_dir" ]]; then
    case "$machine" in
        dk2) build_dir="build" ;;
        *)   build_dir="build-${machine}" ;;
    esac
fi

swu_path="${build_dir}/tmp/deploy/images/${machine}/${image}-${machine}.rootfs.swu"

if [[ ! -f "$swu_path" ]]; then
    echo "Error: SWU file not found: $swu_path" >&2
    exit 1
fi

echo "Uploading ${swu_path} → http://${host}:${port}/upload"
curl -F "file=@${swu_path}" "http://${host}:${port}/upload"
