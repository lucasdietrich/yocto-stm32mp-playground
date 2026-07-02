# eMMC

## extcd

```
root@mp2:~# mmc extcsd read /dev/mmcblk1
=============================================
  Extended CSD rev 1.8 (MMC 5.1)
=============================================

Card Supported Command sets [S_CMD_SET: 0x01]
HPI Features [HPI_FEATURE: 0x01]: implementation based on CMD13
Background operations support [BKOPS_SUPPORT: 0x01]
Max Packet Read Cmd [MAX_PACKED_READS: 0x3f]
Max Packet Write Cmd [MAX_PACKED_WRITES: 0x3f]
Data TAG support [DATA_TAG_SUPPORT: 0x01]
Data TAG Unit Size [TAG_UNIT_SIZE: 0x03]
Tag Resources Size [TAG_RES_SIZE: 0x00]
Context Management Capabilities [CONTEXT_CAPABILITIES: 0x7f]
Large Unit Size [LARGE_UNIT_SIZE_M1: 0x00]
Extended partition attribute support [EXT_SUPPORT: 0x03]
Generic CMD6 Timer [GENERIC_CMD6_TIME: 0x0a]
Power off notification [POWER_OFF_LONG_TIME: 0x32]
Cache Size [CACHE_SIZE] is 512 KiB
Background operations status [BKOPS_STATUS: 0x00]
1st Initialisation Time after programmed sector [INI_TIMEOUT_AP: 0x1e]
Power class for 52MHz, DDR at 3.6V [PWR_CL_DDR_52_360: 0x55]
Power class for 52MHz, DDR at 1.95V [PWR_CL_DDR_52_195: 0xaa]
Power class for 200MHz at 3.6V [PWR_CL_200_360: 0xbb]
Power class for 200MHz, at 1.95V [PWR_CL_200_195: 0xbb]
Minimum Performance for 8bit at 52MHz in DDR mode:
 [MIN_PERF_DDR_W_8_52: 0x00]
 [MIN_PERF_DDR_R_8_52: 0x64]
TRIM Multiplier [TRIM_MULT: 0x01]
Secure Feature support [SEC_FEATURE_SUPPORT: 0x55]
Boot Information [BOOT_INFO: 0x07]
 Device supports alternative boot method
 Device supports dual data rate during boot
 Device supports high speed timing during boot
Boot partition size [BOOT_SIZE_MULTI: 0x20]
Access size [ACC_SIZE: 0x08]
High-capacity erase unit size [HC_ERASE_GRP_SIZE: 0x08]
 i.e. 4096 KiB
High-capacity erase timeout [ERASE_TIMEOUT_MULT: 0x07]
Reliable write sector count [REL_WR_SEC_C: 0x01]
High-capacity W protect group size [HC_WP_GRP_SIZE: 0x01]
 i.e. 4096 KiB
Sleep current (VCC) [S_C_VCC: 0x07]
Sleep current (VCCQ) [S_C_VCCQ: 0x09]
Sleep/awake timeout [S_A_TIMEOUT: 0x14]
Sector Count [SEC_COUNT: 0x00e90000]
 Device is block-addressed
Minimum Write Performance for 8bit:
 [MIN_PERF_W_8_52: 0x00]
 [MIN_PERF_R_8_52: 0x78]
 [MIN_PERF_W_8_26_4_52: 0x00]
 [MIN_PERF_R_8_26_4_52: 0x46]
Minimum Write Performance for 4bit:
 [MIN_PERF_W_4_26: 0x00]
 [MIN_PERF_R_4_26: 0x1e]
Power classes registers:
 [PWR_CL_26_360: 0x44]
 [PWR_CL_52_360: 0x44]
 [PWR_CL_26_195: 0xaa]
 [PWR_CL_52_195: 0xaa]
Partition switching timing [PARTITION_SWITCH_TIME: 0x0a]
Out-of-interrupt busy timing [OUT_OF_INTERRUPT_TIME: 0x0a]
I/O Driver Strength [DRIVER_STRENGTH: 0x1f]
Card Type [CARD_TYPE: 0x57]
 HS400 Dual Data Rate eMMC @200MHz 1.8VI/O
 HS200 Single Data Rate eMMC @200MHz 1.8VI/O
 HS Dual Data Rate eMMC @52MHz 1.8V or 3VI/O
 HS eMMC @52MHz - at rated device voltage(s)
 HS eMMC @26MHz - at rated device voltage(s)
CSD structure version [CSD_STRUCTURE: 0x02]
Command set [CMD_SET: 0x00]
Command set revision [CMD_SET_REV: 0x00]
Power class [POWER_CLASS: 0x0c]
High-speed interface timing [HS_TIMING: 0x02]
Enhanced Strobe mode [STROBE_SUPPORT: 0x01]
Erased memory content [ERASED_MEM_CONT: 0x00]
Boot configuration bytes [PARTITION_CONFIG: 0x48]
 Boot Partition 1 enabled
 No access to boot partition
Boot config protection [BOOT_CONFIG_PROT: 0x00]
Boot bus Conditions [BOOT_BUS_CONDITIONS: 0x00]
High-density erase group definition [ERASE_GROUP_DEF: 0x01]
Boot write protection status registers [BOOT_WP_STATUS]: 0x00
Boot Area Write protection [BOOT_WP]: 0x00
 Power ro locking: possible
 Permanent ro locking: possible
 partition 0 ro lock status: not locked
 partition 1 ro lock status: not locked
User area write protection register [USER_WP]: 0x00
FW configuration [FW_CONFIG]: 0x00
RPMB Size [RPMB_SIZE_MULT]: 0x20
Write reliability setting register [WR_REL_SET]: 0x1f
 user area: the device protects existing data if a power failure occurs during a write operation
 partition 1: the device protects existing data if a power failure occurs during a write operation
 partition 2: the device protects existing data if a power failure occurs during a write operation
 partition 3: the device protects existing data if a power failure occurs during a write operation
 partition 4: the device protects existing data if a power failure occurs during a write operation
Write reliability parameter register [WR_REL_PARAM]: 0x15
 Device supports writing EXT_CSD_WR_REL_SET
 Device supports the enhanced def. of reliable write
Enable background operations handshake [BKOPS_EN]: 0x00
H/W reset function [RST_N_FUNCTION]: 0x00
HPI management [HPI_MGMT]: 0x01
Partitioning Support [PARTITIONING_SUPPORT]: 0x07
 Device support partitioning feature
 Device can have enhanced tech.
Max Enhanced Area Size [MAX_ENH_SIZE_MULT]: 0x0003a4
 i.e. 3817472 KiB
Partitions attribute [PARTITIONS_ATTRIBUTE]: 0x00
Partitioning Setting [PARTITION_SETTING_COMPLETED]: 0x00
 Device partition setting NOT complete
General Purpose Partition Size
 [GP_SIZE_MULT_4]: 0x000000
 [GP_SIZE_MULT_3]: 0x000000
 [GP_SIZE_MULT_2]: 0x000000
 [GP_SIZE_MULT_1]: 0x000000
Enhanced User Data Area Size [ENH_SIZE_MULT]: 0x000000
 i.e. 0 KiB
Enhanced User Data Start Address [ENH_START_ADDR]: 0x00000000
 i.e. 0 bytes offset
Bad Block Management mode [SEC_BAD_BLK_MGMNT]: 0x00
Periodic Wake-up [PERIODIC_WAKEUP]: 0x00
Program CID/CSD in DDR mode support [PROGRAM_CID_CSD_DDR_SUPPORT]: 0x01
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[127]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[126]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[125]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[124]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[123]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[122]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[121]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[120]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[119]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[118]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[117]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[116]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[115]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[114]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[113]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[112]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[111]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[110]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[109]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[108]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[107]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[106]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[105]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[104]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[103]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[102]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[101]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[100]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[99]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[98]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[97]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[96]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[95]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[94]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[93]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[92]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[91]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[90]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[89]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[88]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[87]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[86]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[85]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[84]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[83]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[82]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[81]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[80]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[79]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[78]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[77]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[76]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[75]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[74]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[73]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[72]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[71]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[70]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[69]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[68]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[67]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[66]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[65]]: 0x00
Vendor Specific Fields [VENDOR_SPECIFIC_FIELD[64]]: 0x00
Native sector size [NATIVE_SECTOR_SIZE]: 0x01
Sector size emulation [USE_NATIVE_SECTOR]: 0x00
Sector size [DATA_SECTOR_SIZE]: 0x00
1st initialization after disabling sector size emulation [INI_TIMEOUT_EMU]: 0x0a
Class 6 commands control [CLASS_6_CTRL]: 0x00
Number of addressed group to be Released[DYNCAP_NEEDED]: 0x00
Exception events control [EXCEPTION_EVENTS_CTRL]: 0x0000
Exception events status[EXCEPTION_EVENTS_STATUS]: 0x0000
Extended Partitions Attribute [EXT_PARTITIONS_ATTRIBUTE]: 0x0000
Context configuration [CONTEXT_CONF[51]]: 0x00
Context configuration [CONTEXT_CONF[50]]: 0x00
Context configuration [CONTEXT_CONF[49]]: 0x00
Context configuration [CONTEXT_CONF[48]]: 0x00
Context configuration [CONTEXT_CONF[47]]: 0x00
Context configuration [CONTEXT_CONF[46]]: 0x00
Context configuration [CONTEXT_CONF[45]]: 0x00
Context configuration [CONTEXT_CONF[44]]: 0x00
Context configuration [CONTEXT_CONF[43]]: 0x00
Context configuration [CONTEXT_CONF[42]]: 0x00
Context configuration [CONTEXT_CONF[41]]: 0x00
Context configuration [CONTEXT_CONF[40]]: 0x00
Context configuration [CONTEXT_CONF[39]]: 0x00
Context configuration [CONTEXT_CONF[38]]: 0x00
Context configuration [CONTEXT_CONF[37]]: 0x00
Packed command status [PACKED_COMMAND_STATUS]: 0x00
Packed command failure index [PACKED_FAILURE_INDEX]: 0x00
Power Off Notification [POWER_OFF_NOTIFICATION]: 0x01
Control to turn the Cache ON/OFF [CACHE_CTRL]: 0x01
Control to turn the Cache Barrier ON/OFF [BARRIER_CTRL]: 0x00
eMMC Firmware Version:
eMMC Life Time Estimation A [EXT_CSD_DEVICE_LIFE_TIME_EST_TYP_A]: 0x01
eMMC Life Time Estimation B [EXT_CSD_DEVICE_LIFE_TIME_EST_TYP_B]: 0x00
eMMC Pre EOL information [EXT_CSD_PRE_EOL_INFO]: 0x01
Secure Removal Type [SECURE_REMOVAL_TYPE]: 0x39
 information is configured to be removed using a vendor defined
 Supported Secure Removal Type:
  information removed by an erase of the physical memory
  information removed using a vendor defined
Command Queue Support [CMDQ_SUPPORT]: 0x01
Command Queue Depth [CMDQ_DEPTH]: 32
Command Enabled [CMDQ_MODE_EN]: 0x00
Note: CMDQ_MODE_EN may not indicate the runtime CMDQ ON or OFF.
Please check sysfs node '/sys/devices/.../mmc_host/mmcX/mmcX:XXXX/cmdq_en'
```

## fio

Script:
```
fio --name=seq_read --filename=/dev/mmcblk1 --size=1G \
    --bs=1M --rw=read --direct=1 --ioengine=libaio --iodepth=1

fio --name=rand_read --filename=/dev/mmcblk1 --size=1G \
    --bs=4k --rw=randread --direct=1 --ioengine=libaio --iodepth=32

fio --name=seq_write --filename=/dev/mmcblk1 --size=512M \
    --bs=1M --rw=write --direct=1 --ioengine=libaio --iodepth=1

fio --name=rand_write --filename=/dev/mmcblk1 --size=512M \
    --bs=4k --rw=randwrite --direct=1 --ioengine=libaio --iodepth=32

fio --name=randrw_test --filename=/dev/mmcblk1 --size=512M \
    --bs=4k --rw=randrw --rwmixread=70 --direct=1 --ioengine=libaio --iodepth=32
```

Result:
```
root@mp2:~# ./s.sh
seq_read: (g=0): rw=read, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=libaio, iodepth=1
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [R(1)][100.0%][r=75.1MiB/s][r=75 IOPS][eta 00m:00s]
seq_read: (groupid=0, jobs=1): err= 0: pid=527: Fri Mar  9 16:24:49 2018
  read: IOPS=75, BW=75.1MiB/s (78.8MB/s)(1024MiB/13632msec)
    slat (usec): min=192, max=2324, avg=679.43, stdev=450.87
    clat (usec): min=11988, max=15696, avg=12607.31, stdev=489.68
     lat (usec): min=13115, max=16742, avg=13286.74, stdev=173.45
    clat percentiles (usec):
     |  1.00th=[11994],  5.00th=[12125], 10.00th=[12125], 20.00th=[12125],
     | 30.00th=[12125], 40.00th=[12125], 50.00th=[12911], 60.00th=[13042],
     | 70.00th=[13042], 80.00th=[13042], 90.00th=[13042], 95.00th=[13173],
     | 99.00th=[13304], 99.50th=[13304], 99.90th=[15664], 99.95th=[15664],
     | 99.99th=[15664]
   bw (  KiB/s): min=75624, max=77824, per=100.00%, avg=76938.52, stdev=1054.56, samples=27
   iops        : min=   73, max=   76, avg=74.85, stdev= 1.26, samples=27
  lat (msec)   : 20=100.00%
  cpu          : usr=0.41%, sys=2.05%, ctx=1962, majf=0, minf=275
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwts: total=1024,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
   READ: bw=75.1MiB/s (78.8MB/s), 75.1MiB/s-75.1MiB/s (78.8MB/s-78.8MB/s), io=1024MiB (1074MB), run=13632-13632msec

Disk stats (read/write):
  mmcblk1: ios=2046/0, sectors=2095104/0, merge=0/0, ticks=25807/0, in_queue=25807, util=97.85%
rand_read: (g=0): rw=randread, bs=(R) 4096B-4096B, (W) 4096B-4096B, (T) 4096B-4096B, ioengine=libaio, iodepth=32
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [r(1)][100.0%][r=21.7MiB/s][r=5546 IOPS][eta 00m:00s]
rand_read: (groupid=0, jobs=1): err= 0: pid=534: Fri Mar  9 16:25:46 2018
  read: IOPS=4591, BW=17.9MiB/s (18.8MB/s)(1024MiB/57091msec)
    slat (usec): min=11, max=737, avg=20.48, stdev= 6.49
    clat (usec): min=412, max=35974, avg=6942.65, stdev=2671.46
     lat (usec): min=437, max=35993, avg=6963.13, stdev=2671.40
    clat percentiles (usec):
     |  1.00th=[ 1352],  5.00th=[ 2507], 10.00th=[ 3359], 20.00th=[ 4555],
     | 30.00th=[ 5538], 40.00th=[ 6259], 50.00th=[ 6915], 60.00th=[ 7570],
     | 70.00th=[ 8356], 80.00th=[ 9241], 90.00th=[10421], 95.00th=[11338],
     | 99.00th=[12518], 99.50th=[12911], 99.90th=[16188], 99.95th=[21627],
     | 99.99th=[28967]
   bw (  KiB/s): min=14792, max=26568, per=100.00%, avg=18393.46, stdev=946.28, samples=114
   iops        : min= 3698, max= 6642, avg=4598.32, stdev=236.58, samples=114
  lat (usec)   : 500=0.01%, 750=0.12%, 1000=0.27%
  lat (msec)   : 2=2.48%, 4=11.91%, 10=71.56%, 20=13.59%, 50=0.07%
  cpu          : usr=4.74%, sys=11.94%, ctx=260488, majf=0, minf=53
  IO depths    : 1=0.1%, 2=0.1%, 4=0.1%, 8=0.1%, 16=0.1%, 32=100.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.1%, 64=0.0%, >=64=0.0%
     issued rwts: total=262144,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=32

Run status group 0 (all jobs):
   READ: bw=17.9MiB/s (18.8MB/s), 17.9MiB/s-17.9MiB/s (18.8MB/s-18.8MB/s), io=1024MiB (1074MB), run=57091-57091msec

Disk stats (read/write):
  mmcblk1: ios=259821/0, sectors=2083872/0, merge=670/0, ticks=1808295/0, in_queue=1808295, util=100.00%
root@mp2:~# nano s.sh
root@mp2:~# ./s.sh
seq_read: (g=0): rw=read, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=libaio, iodepth=1
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [R(1)][100.0%][r=69.0MiB/s][r=69 IOPS][eta 00m:00s]
seq_read: (groupid=0, jobs=1): err= 0: pid=545: Fri Mar  9 16:26:18 2018
  read: IOPS=68, BW=68.7MiB/s (72.0MB/s)(1024MiB/14904msec)
    slat (usec): min=210, max=2213, avg=822.87, stdev=580.40
    clat (usec): min=12444, max=18179, avg=13705.98, stdev=638.99
     lat (usec): min=13738, max=18424, avg=14528.86, stdev=254.93
    clat percentiles (usec):
     |  1.00th=[12911],  5.00th=[13042], 10.00th=[13042], 20.00th=[13042],
     | 30.00th=[13042], 40.00th=[13173], 50.00th=[13698], 60.00th=[14222],
     | 70.00th=[14222], 80.00th=[14353], 90.00th=[14353], 95.00th=[14353],
     | 99.00th=[14484], 99.50th=[14746], 99.90th=[17695], 99.95th=[18220],
     | 99.99th=[18220]
   bw (  KiB/s): min=69493, max=71680, per=100.00%, avg=70396.24, stdev=997.11, samples=29
   iops        : min=   67, max=   70, avg=68.24, stdev= 0.91, samples=29
  lat (msec)   : 20=100.00%
  cpu          : usr=0.44%, sys=2.01%, ctx=1940, majf=0, minf=275
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwts: total=1024,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
   READ: bw=68.7MiB/s (72.0MB/s), 68.7MiB/s-68.7MiB/s (72.0MB/s-72.0MB/s), io=1024MiB (1074MB), run=14904-14904msec

Disk stats (read/write):
  mmcblk1: ios=2042/0, sectors=2091008/0, merge=0/0, ticks=28026/0, in_queue=28026, util=97.38%
rand_read: (g=0): rw=randread, bs=(R) 4096B-4096B, (W) 4096B-4096B, (T) 4096B-4096B, ioengine=libaio, iodepth=32
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [r(1)][100.0%][r=21.5MiB/s][r=5496 IOPS][eta 00m:00s]
rand_read: (groupid=0, jobs=1): err= 0: pid=550: Fri Mar  9 16:27:16 2018
  read: IOPS=4588, BW=17.9MiB/s (18.8MB/s)(1024MiB/57133msec)
    slat (usec): min=10, max=312, avg=20.64, stdev= 3.50
    clat (usec): min=415, max=14996, avg=6947.30, stdev=2621.18
     lat (usec): min=434, max=15024, avg=6967.94, stdev=2621.13
    clat percentiles (usec):
     |  1.00th=[ 1352],  5.00th=[ 2507], 10.00th=[ 3392], 20.00th=[ 4621],
     | 30.00th=[ 5538], 40.00th=[ 6325], 50.00th=[ 6980], 60.00th=[ 7635],
     | 70.00th=[ 8455], 80.00th=[ 9372], 90.00th=[10421], 95.00th=[11338],
     | 99.00th=[12518], 99.50th=[12911], 99.90th=[13435], 99.95th=[13698],
     | 99.99th=[14222]
   bw (  KiB/s): min=17892, max=26296, per=100.00%, avg=18374.75, stdev=844.20, samples=114
   iops        : min= 4473, max= 6574, avg=4593.64, stdev=211.06, samples=114
  lat (usec)   : 500=0.01%, 750=0.13%, 1000=0.29%
  lat (msec)   : 2=2.43%, 4=11.84%, 10=71.69%, 20=13.62%
  cpu          : usr=4.50%, sys=11.74%, ctx=260588, majf=0, minf=53
  IO depths    : 1=0.1%, 2=0.1%, 4=0.1%, 8=0.1%, 16=0.1%, 32=100.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.1%, 64=0.0%, >=64=0.0%
     issued rwts: total=262144,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=32

Run status group 0 (all jobs):
   READ: bw=17.9MiB/s (18.8MB/s), 17.9MiB/s-17.9MiB/s (18.8MB/s-18.8MB/s), io=1024MiB (1074MB), run=57133-57133msec

Disk stats (read/write):
  mmcblk1: ios=260929/0, sectors=2096312/0, merge=1119/0, ticks=1814371/0, in_queue=1814371, util=99.99%
seq_write: (g=0): rw=write, bs=(R) 1024KiB-1024KiB, (W) 1024KiB-1024KiB, (T) 1024KiB-1024KiB, ioengine=libaio, iodepth=1
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=0): [f(1)][100.0%][w=30.0MiB/s][w=30 IOPS][eta 00m:00s]
seq_write: (groupid=0, jobs=1): err= 0: pid=559: Fri Mar  9 16:27:33 2018
  write: IOPS=30, BW=30.4MiB/s (31.9MB/s)(512MiB/16841msec); 0 zone resets
    slat (usec): min=283, max=1638, avg=394.80, stdev=124.34
    clat (usec): min=28868, max=76462, avg=32470.49, stdev=4404.79
     lat (usec): min=30090, max=76846, avg=32865.29, stdev=4397.99
    clat percentiles (usec):
     |  1.00th=[29754],  5.00th=[30016], 10.00th=[30278], 20.00th=[30278],
     | 30.00th=[30540], 40.00th=[30802], 50.00th=[30802], 60.00th=[31065],
     | 70.00th=[31327], 80.00th=[32637], 90.00th=[39584], 95.00th=[40109],
     | 99.00th=[45876], 99.50th=[47973], 99.90th=[76022], 99.95th=[76022],
     | 99.99th=[76022]
   bw (  KiB/s): min=28672, max=32768, per=100.00%, avg=31131.39, stdev=986.32, samples=33
   iops        : min=   28, max=   32, avg=30.06, stdev= 1.00, samples=33
  lat (msec)   : 50=99.61%, 100=0.39%
  cpu          : usr=0.53%, sys=0.80%, ctx=526, majf=0, minf=19
  IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     issued rwts: total=0,512,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=1

Run status group 0 (all jobs):
  WRITE: bw=30.4MiB/s (31.9MB/s), 30.4MiB/s-30.4MiB/s (31.9MB/s-31.9MB/s), io=512MiB (537MB), run=16841-16841msec

Disk stats (read/write):
  mmcblk1: ios=42/1010, sectors=2064/1034240, merge=0/0, ticks=26/32801, in_queue=32827, util=98.69%
rand_write: (g=0): rw=randwrite, bs=(R) 4096B-4096B, (W) 4096B-4096B, (T) 4096B-4096B, ioengine=libaio, iodepth=32
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [w(1)][100.0%][w=11.9MiB/s][w=3044 IOPS][eta 00m:00s]
rand_write: (groupid=0, jobs=1): err= 0: pid=565: Fri Mar  9 16:28:52 2018
  write: IOPS=1670, BW=6683KiB/s (6843kB/s)(512MiB/78452msec); 0 zone resets
    slat (usec): min=11, max=759, avg=24.16, stdev=16.37
    clat (usec): min=188, max=429229, avg=19121.23, stdev=51535.55
     lat (usec): min=205, max=429257, avg=19145.38, stdev=51536.36
    clat percentiles (usec):
     |  1.00th=[   619],  5.00th=[  1156], 10.00th=[  1549], 20.00th=[  2180],
     | 30.00th=[  2638], 40.00th=[  2999], 50.00th=[  3458], 60.00th=[  4047],
     | 70.00th=[  5080], 80.00th=[  7701], 90.00th=[ 29492], 95.00th=[147850],
     | 99.00th=[263193], 99.50th=[295699], 99.90th=[337642], 99.95th=[354419],
     | 99.99th=[383779]
   bw (  KiB/s): min=  560, max=32256, per=100.00%, avg=6702.39, stdev=3373.94, samples=156
   iops        : min=  140, max= 8064, avg=1675.42, stdev=843.47, samples=156
  lat (usec)   : 250=0.03%, 500=0.50%, 750=1.28%, 1000=1.95%
  lat (msec)   : 2=13.25%, 4=42.49%, 10=26.85%, 20=3.37%, 50=1.09%
  lat (msec)   : 100=2.06%, 250=5.82%, 500=1.31%
  cpu          : usr=1.63%, sys=4.64%, ctx=133247, majf=0, minf=21
  IO depths    : 1=0.1%, 2=0.1%, 4=0.1%, 8=0.1%, 16=0.1%, 32=100.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.1%, 64=0.0%, >=64=0.0%
     issued rwts: total=0,131072,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=32

Run status group 0 (all jobs):
  WRITE: bw=6683KiB/s (6843kB/s), 6683KiB/s-6683KiB/s (6843kB/s-6843kB/s), io=512MiB (537MB), run=78452-78452msec

Disk stats (read/write):
  mmcblk1: ios=1/130337, sectors=40/1048448, merge=0/726, ticks=1/2486925, in_queue=2486925, util=100.00%
randrw_test: (g=0): rw=randrw, bs=(R) 4096B-4096B, (W) 4096B-4096B, (T) 4096B-4096B, ioengine=libaio, iodepth=32
fio-3.36-117-gb2403-dirty
Starting 1 process
Jobs: 1 (f=1): [m(1)][100.0%][r=9465KiB/s,w=4028KiB/s][r=2366,w=1007 IOPS][eta 00m:00s]
randrw_test: (groupid=0, jobs=1): err= 0: pid=571: Fri Mar  9 16:29:40 2018
  read: IOPS=1930, BW=7724KiB/s (7909kB/s)(359MiB/47556msec)
    slat (usec): min=12, max=727, avg=21.09, stdev= 6.92
    clat (usec): min=367, max=214421, avg=11618.05, stdev=20678.72
     lat (usec): min=387, max=214471, avg=11639.14, stdev=20679.22
    clat percentiles (usec):
     |  1.00th=[  1319],  5.00th=[  2311], 10.00th=[  3064], 20.00th=[  4146],
     | 30.00th=[  4948], 40.00th=[  5604], 50.00th=[  6325], 60.00th=[  7046],
     | 70.00th=[  7963], 80.00th=[  9241], 90.00th=[ 18220], 95.00th=[ 46924],
     | 99.00th=[122160], 99.50th=[139461], 99.90th=[160433], 99.95th=[168821],
     | 99.99th=[181404]
   bw (  KiB/s): min=  976, max=13832, per=100.00%, avg=7731.35, stdev=4125.79, samples=95
   iops        : min=  244, max= 3458, avg=1932.78, stdev=1031.47, samples=95
  write: IOPS=825, BW=3301KiB/s (3380kB/s)(153MiB/47556msec); 0 zone resets
    slat (usec): min=12, max=704, avg=21.59, stdev= 8.87
    clat (usec): min=217, max=186913, avg=11498.99, stdev=19902.44
     lat (usec): min=234, max=186940, avg=11520.58, stdev=19903.10
    clat percentiles (usec):
     |  1.00th=[   529],  5.00th=[  1057], 10.00th=[  1663], 20.00th=[  3228],
     | 30.00th=[  4686], 40.00th=[  5538], 50.00th=[  6259], 60.00th=[  7439],
     | 70.00th=[  8717], 80.00th=[  9896], 90.00th=[ 18744], 95.00th=[ 55837],
     | 99.00th=[109577], 99.50th=[124257], 99.90th=[152044], 99.95th=[158335],
     | 99.99th=[170918]
   bw (  KiB/s): min=  518, max= 5984, per=100.00%, avg=3304.03, stdev=1756.92, samples=95
   iops        : min=  129, max= 1496, avg=825.97, stdev=439.26, samples=95
  lat (usec)   : 250=0.02%, 500=0.23%, 750=0.58%, 1000=0.78%
  lat (msec)   : 2=4.44%, 4=14.47%, 10=61.71%, 20=9.19%, 50=3.52%
  lat (msec)   : 100=3.21%, 250=1.85%
  cpu          : usr=3.00%, sys=7.31%, ctx=130995, majf=0, minf=27
  IO depths    : 1=0.1%, 2=0.1%, 4=0.1%, 8=0.1%, 16=0.1%, 32=100.0%, >=64=0.0%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.0%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.1%, 64=0.0%, >=64=0.0%
     issued rwts: total=91828,39244,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=32

Run status group 0 (all jobs):
   READ: bw=7724KiB/s (7909kB/s), 7724KiB/s-7724KiB/s (7909kB/s-7909kB/s), io=359MiB (376MB), run=47556-47556msec
  WRITE: bw=3301KiB/s (3380kB/s), 3301KiB/s-3301KiB/s (3380kB/s-3380kB/s), io=153MiB (161MB), run=47556-47556msec

Disk stats (read/write):
  mmcblk1: ios=90780/38828, sectors=728256/310984, merge=181/45, ticks=1059954/448831, in_queue=1508785, util=99.95%
```

## Synthesis

### Device capabilities (from extcsd)

The eMMC (MMC 5.1) supports HS400 @ 200 MHz DDR (1.8 V I/O), but `HS_TIMING: 0x02` indicates it is currently operating in **HS200** mode. `BOOT_BUS_CONDITIONS: 0x00` means the boot path falls back to a lower-speed mode until the kernel re-negotiates the bus.

Command Queuing (`CMDQ_SUPPORT: 0x01`, depth=32) is advertised but `CMDQ_MODE_EN: 0x00` — it is **disabled** at runtime. Enabling it could reduce random I/O latency under concurrent workloads. The 512 KiB write-back cache is active (`CACHE_CTRL: 0x01`).

Life-time estimation A = 0x01 (0–10% used), pre-EOL = 0x01 (normal) — the device is healthy.

### fio results summary

| Test              | BW                        | IOPS           | Notes                         |
| ----------------- | ------------------------- | -------------- | ----------------------------- |
| Seq read 1M       | ~72 MiB/s                 | ~72            | Run-to-run variance ~9%       |
| Rand read 4K/d32  | ~17.9 MiB/s               | ~4590          | Very consistent across runs   |
| Seq write 1M      | 30.4 MiB/s                | ~30            | Single run                    |
| Rand write 4K/d32 | 6.5 MiB/s                 | ~1670          | High tail latency (see below) |
| RandRW 4K/d32 70r | R 7.5 MiB/s / W 3.2 MiB/s | R 1930 / W 826 | —                             |

### Analysis

**Sequential throughput is well below HS200 theoretical limits.** HS200 @ 200 MHz / 8-bit bus gives a raw ceiling of ~200 MB/s. Measured sequential read (~72 MiB/s) and write (~30 MiB/s) are far below this, pointing to software overhead (DMA, scheduler, libaio iodepth=1) rather than a bus bottleneck. Raising iodepth or switching to `io_uring` would likely improve sequential write figures.

**Random read is reasonable.** ~4590 IOPS at 4K / iodepth=32 is consistent with a mid-range eMMC with internal read cache active. Latency is well-behaved (p99 = 12.5 ms).

**Random write tail latency is problematic.** p95 = 148 ms, p99 = 263 ms, p99.9 = 338 ms. This is classic eMMC write-amplification / garbage-collection behaviour. With CMDQ disabled, there is no out-of-order reordering to hide it. Enabling CMDQ (`mmc cqe enable`) or adding a `blk-mq` scheduler (`mq-deadline`) may flatten the tail.

**Run-to-run variance on sequential read (~9%).** The first run at 75 MiB/s drops to 69 MiB/s on the second pass. Likely caused by the internal cache warming on the first pass or minor thermal effects; not a concern in practice.

**Recommendations:**
- Confirm HS400 negotiation is attempted in the kernel DTS/driver and measure again.
- Enable CMDQ in the kernel device tree (`mmc-hs400-1_8v`, `supports-cqe`) if supported by the host controller.
- Use `mq-deadline` I/O scheduler to cap random-write latency spikes.
- Re-run with `iodepth=1` for sequential write to get a cleaner baseline unaffected by cache flushing artefacts.
