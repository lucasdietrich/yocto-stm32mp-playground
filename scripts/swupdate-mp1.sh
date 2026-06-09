
#!/bin/bash

machine="dk2"
image=amy-image
swu_path="build/tmp/deploy/images/${machine}/${image}-${machine}.rootfs.swu"

ip="192.168.10.219"
port="8087"

curl -F "file=@${swu_path}" http://${ip}:${port}/upload