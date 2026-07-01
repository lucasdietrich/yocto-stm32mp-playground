Build repo:

```
mkdir build && cd build
source /opt/amy/mp2-1.0/environment-setup-cortexa35-poky-linux
cmake  -DCMAKE_INSTALL_PREFIX=../install -DBUILD_SHARED_LIBS=ON ..
make
make install
```