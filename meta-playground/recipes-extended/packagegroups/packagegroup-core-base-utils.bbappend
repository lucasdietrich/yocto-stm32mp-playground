# kea-dhcp4-server is enabled unconditionally by this packagegroup on
# non-systemd distros, running as an unconfigured second DHCP server
# alongside dnsmasq (see meta-playground packagegroup-amy-core.bb).
RDEPENDS:${PN}:remove = "kea"
