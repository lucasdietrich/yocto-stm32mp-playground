/*
 * led_button_ctrl.c
 *
 * Listens on the gpio-keys input device for BTN_1 and BTN_2 presses:
 *   - BTN_1 pressed -> turn the blue LED on
 *   - BTN_2 pressed -> turn the blue LED off
 *
 * Usage:
 *   led_button_ctrl [input_device] [led_brightness_path]
 *
 * Defaults:
 *   input_device        = /dev/input/event0
 *   led_brightness_path = /sys/class/leds/blue:heartbeat/brightness
 *
 * Note: button-user-2 must be enabled (status = "okay") in the
 * device tree for BTN_2 events to be generated.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <linux/input.h>

#define DEFAULT_INPUT_DEV "/dev/input/event0"
#define DEFAULT_LED_PATH  "/sys/class/leds/blue:heartbeat/brightness"

static int set_led(const char *led_path, int on)
{
    int fd = open(led_path, O_WRONLY);
    if (fd < 0) {
        fprintf(stderr, "Failed to open %s: %s\n", led_path, strerror(errno));
        return -1;
    }

    const char *value = on ? "1" : "0";
    ssize_t n = write(fd, value, 1);
    close(fd);

    if (n != 1) {
        fprintf(stderr, "Failed to write to %s: %s\n", led_path, strerror(errno));
        return -1;
    }

    return 0;
}

int main(int argc, char *argv[])
{
    const char *input_dev = (argc > 1) ? argv[1] : DEFAULT_INPUT_DEV;
    const char *led_path  = (argc > 2) ? argv[2] : DEFAULT_LED_PATH;

    int fd = open(input_dev, O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "Failed to open %s: %s\n", input_dev, strerror(errno));
        return EXIT_FAILURE;
    }

    printf("Listening on %s\n", input_dev);
    printf("Controlling LED at %s\n", led_path);
    printf("BTN_1 -> LED on, BTN_2 -> LED off\n");

    struct input_event ev;

    while (1) {
        ssize_t n = read(fd, &ev, sizeof(ev));

        if (n < 0) {
            fprintf(stderr, "Read error: %s\n", strerror(errno));
            break;
        }

        if (n != (ssize_t)sizeof(ev)) {
            continue;
        }

        /* Only react on key press (value == 1), ignore release/repeat */
        if (ev.type != EV_KEY || ev.value != 1) {
            continue;
        }

        if (ev.code == BTN_1) {
            printf("BTN_1 pressed -> LED on\n");
            set_led(led_path, 1);
        } else if (ev.code == BTN_2) {
            printf("BTN_2 pressed -> LED off\n");
            set_led(led_path, 0);
        }
    }

    close(fd);
    return EXIT_SUCCESS;
}