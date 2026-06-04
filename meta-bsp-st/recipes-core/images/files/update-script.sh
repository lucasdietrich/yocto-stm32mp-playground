#!/bin/sh

set -e

# get first argument of the script ('preinst', 'postinst' or 'postfailure')
stage="$1"

case "$stage" in
    preinst)
        echo "Running pre-installation script..."
        # Add any pre-installation commands here
        ;;
    postinst)
        echo "Running post-installation script..."
        # Add any post-installation commands here

        fwupdate apply_new_update || {
            echo "Failed to apply new update. Exiting."
            exit 1
        }

        ;;
    postfailure)
        echo "Running post-failure script..."
        # Add any post-failure commands here

        fwupdate abort_update || {
            echo "Failed to abort update. Exiting."
            exit 1
        }

        ;;
    *)
        echo "Unknown stage: $stage"
        exit 1
        ;;
esac