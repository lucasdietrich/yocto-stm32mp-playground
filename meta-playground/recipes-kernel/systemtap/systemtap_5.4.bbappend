# nativesdk scripts (dtrace, stap-profile-annotate, stap-exporter) are
# patched (0001-Do-not-let-configure-write-a-python-location-into-th.patch)
# to shebang literal /usr/bin/python3 on purpose, so they run using the
# SDK host's own python3 instead of a build-path-dependent interpreter.
# nativesdk-python3 installs under SDKPATHNATIVE, not /usr/bin, so
# file-rdeps can never find a provider for /usr/bin/python3 here - false
# positive, host is assumed to provide python3.
INSANE_SKIP:${PN}:class-nativesdk += "file-rdeps"
INSANE_SKIP:${PN}-python:class-nativesdk += "file-rdeps"
INSANE_SKIP:${PN}-exporter:class-nativesdk += "file-rdeps"

# Upstream already skips this for the target build (RDEPENDS:${PN}-python
# on ${PN}-dev is intentional, python bindings need the sdt headers), but
# INSANE_SKIP isn't in PACKAGEVARS so it doesn't carry over to the
# nativesdk BBCLASSEXTEND variant automatically.
INSANE_SKIP:${PN}-python:class-nativesdk += "dev-deps"
