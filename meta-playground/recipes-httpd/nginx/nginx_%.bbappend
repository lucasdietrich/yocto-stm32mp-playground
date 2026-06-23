FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "\
    file://default_server.site \
    file://gateway.site \
    file://mqtt.conf \
    file://index.html \
"

# nginx features
PACKAGECONFIG += "ssl"
PACKAGECONFIG += "stream"
PACKAGECONFIG += "http-auth-request"
PACKAGECONFIG += "stream_ssl"
PACKAGECONFIG += "gateway"


# for SSL in stream { }
# - http://nginx.org/en/docs/stream/ngx_stream_ssl_module.html
PACKAGECONFIG[stream_ssl] = "--with-stream_ssl_module,,"
PACKAGECONFIG[gateway] = ""

do_install:append() {
    if [ "${@bb.utils.contains('PACKAGECONFIG', 'gateway', '1', '0', d)}" = "1" ]; then
        # install gateway site
        install -Dm 0644 ${WORKDIR}/gateway.site ${D}${sysconfdir}/nginx/sites-available/gateway
        ln -s ../sites-available/gateway ${D}${sysconfdir}/nginx/sites-enabled/
    fi

    install -Dm 0644 ${WORKDIR}/mqtt.conf ${D}${sysconfdir}/nginx/conf.d/mqtt.conf

    install -Dm 0644 ${WORKDIR}/index.html ${D}/var/www/localhost/html/index.html
}