# Documentation

## USB

Connect USB user to host, it will create an USB NCM Ethernet interface:

- On windows, the host will automatically get an IP `10.77.0.x`
- On debian, you will need to configure `/etc/network/interfaces` to automatically run DHCP client on hot-plug:
    ```
    allow-hotplug enx020000000002
    iface enx020000000002 inet dhcp
    ```

Add following fragment to `~/.ssh/config`:

```
Host mpx.usb
    Hostname mpx.usb
    User root
    IdentityFile ~/.ssh/id_ed25519_board
```