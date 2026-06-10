FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "\
    file://default_server.site \
    file://pycancontroller_server.site \
    file://homeassistant.site \
    file://gateway.site \
    file://mqtt.conf \
"

# nginx features
PACKAGECONFIG += "ssl"
PACKAGECONFIG += "stream"
PACKAGECONFIG += "http-auth-request"
PACKAGECONFIG += "stream_ssl"
PACKAGECONFIG += "gateway"
PACKAGECONFIG += "homeassistant"


# for SSL in stream { }
# - http://nginx.org/en/docs/stream/ngx_stream_ssl_module.html
PACKAGECONFIG[stream_ssl] = "--with-stream_ssl_module,,"
PACKAGECONFIG[pycancontroller] = ""
PACKAGECONFIG[gateway] = ""
PACKAGECONFIG[homeassistant] = ""

do_install:append() {
    # install pycancontroller_server site
    if [ "${@bb.utils.contains('PACKAGECONFIG', 'pycancontroller', '1', '0', d)}" = "1" ]; then
        install -Dm 0644 ${WORKDIR}/pycancontroller_server.site ${D}${sysconfdir}/nginx/sites-available/pycancontroller_server
        ln -s ../sites-available/pycancontroller_server ${D}${sysconfdir}/nginx/sites-enabled/
    fi

    if [ "${@bb.utils.contains('PACKAGECONFIG', 'gateway', '1', '0', d)}" = "1" ]; then
        # install gateway site
        install -Dm 0644 ${WORKDIR}/gateway.site ${D}${sysconfdir}/nginx/sites-available/gateway
        ln -s ../sites-available/gateway ${D}${sysconfdir}/nginx/sites-enabled/
    fi

    if [ "${@bb.utils.contains('PACKAGECONFIG', 'homeassistant', '1', '0', d)}" = "1" ]; then
        # install homeassistant site
        install -Dm 0644 ${WORKDIR}/homeassistant.site ${D}${sysconfdir}/nginx/sites-available/homeassistant
        ln -s ../sites-available/homeassistant ${D}${sysconfdir}/nginx/sites-enabled/
    fi

    install -Dm 0644 ${WORKDIR}/mqtt.conf ${D}${sysconfdir}/nginx/conf.d/mqtt.conf
}