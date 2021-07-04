package su.uac.mountinfo

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MountInfoRecordTest {
    @Test
    fun canParseLinuxMountInfo() {
        val input =
            """
                22 92 0:21 / /proc rw,nosuid,nodev,noexec,relatime shared:13 - proc proc rw
                23 92 0:22 / /sys rw,nosuid,nodev,noexec,relatime shared:2 - sysfs sysfs rw,seclabel
                24 92 0:5 / /dev rw,nosuid shared:9 - devtmpfs devtmpfs rw,seclabel,size=3012448k,nr_inodes=753112,mode=755,inode64
                25 23 0:6 / /sys/kernel/security rw,nosuid,nodev,noexec,relatime shared:3 - securityfs securityfs rw
                26 24 0:23 / /dev/shm rw,nosuid,nodev shared:10 - tmpfs tmpfs rw,seclabel,inode64
                27 24 0:24 / /dev/pts rw,nosuid,noexec,relatime shared:11 - devpts devpts rw,seclabel,gid=5,mode=620,ptmxmode=000
                28 92 0:25 / /run rw,nosuid,nodev shared:12 - tmpfs tmpfs rw,seclabel,size=1212832k,nr_inodes=819200,mode=755,inode64
                29 23 0:26 / /sys/fs/cgroup rw,nosuid,nodev,noexec,relatime shared:4 - cgroup2 cgroup2 rw,seclabel,nsdelegate,memory_recursiveprot
                30 23 0:27 / /sys/fs/pstore rw,nosuid,nodev,noexec,relatime shared:5 - pstore pstore rw,seclabel
                31 23 0:28 / /sys/firmware/efi/efivars rw,nosuid,nodev,noexec,relatime shared:6 - efivarfs efivarfs rw
                32 23 0:29 / /sys/fs/bpf rw,nosuid,nodev,noexec,relatime shared:7 - bpf none rw,mode=700
                92 1 253:0 / / rw,relatime shared:1 - xfs /dev/mapper/fedora_fedora-root rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota
                34 23 0:20 / /sys/fs/selinux rw,nosuid,noexec,relatime shared:8 - selinuxfs selinuxfs rw
                33 22 0:31 / /proc/sys/fs/binfmt_misc rw,relatime shared:14 - autofs systemd-1 rw,fd=31,pgrp=1,timeout=0,minproto=5,maxproto=5,direct,pipe_ino=14026
                35 24 0:19 / /dev/mqueue rw,nosuid,nodev,noexec,relatime shared:15 - mqueue mqueue rw,seclabel
                36 24 0:32 / /dev/hugepages rw,relatime shared:16 - hugetlbfs hugetlbfs rw,seclabel,pagesize=2M
                37 23 0:7 / /sys/kernel/debug rw,nosuid,nodev,noexec,relatime shared:17 - debugfs debugfs rw,seclabel
                38 23 0:12 / /sys/kernel/tracing rw,nosuid,nodev,noexec,relatime shared:18 - tracefs tracefs rw,seclabel
                39 23 0:33 / /sys/fs/fuse/connections rw,nosuid,nodev,noexec,relatime shared:19 - fusectl fusectl rw
                40 23 0:34 / /sys/kernel/config rw,nosuid,nodev,noexec,relatime shared:20 - configfs configfs rw
                87 92 0:35 / /tmp rw,nosuid,nodev shared:40 - tmpfs tmpfs rw,seclabel,size=3032076k,nr_inodes=409600,inode64
                90 92 8:2 / /boot rw,relatime shared:46 - xfs /dev/sda2 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota
                94 92 7:0 / /var/lib/snapd/snap/android-studio/105 ro,nodev,relatime shared:48 - squashfs /dev/loop0 ro,context=system_u:object_r:snappy_snap_t:s0
                97 92 7:2 / /var/lib/snapd/snap/snapd/12159 ro,nodev,relatime shared:50 - squashfs /dev/loop2 ro,context=system_u:object_r:snappy_snap_t:s0
                100 92 7:1 / /var/lib/snapd/snap/core20/1026 ro,nodev,relatime shared:52 - squashfs /dev/loop1 ro,context=system_u:object_r:snappy_snap_t:s0
                103 90 8:1 / /boot/efi rw,relatime shared:54 - vfat /dev/sda1 rw,fmask=0077,dmask=0077,codepage=437,iocharset=ascii,shortname=winnt,errors=remount-ro
                363 92 0:42 / /var/lib/nfs/rpc_pipefs rw,relatime shared:193 - rpc_pipefs sunrpc rw
                688 28 0:45 / /run/user/1000 rw,nosuid,nodev,relatime shared:325 - tmpfs tmpfs rw,seclabel,size=606412k,nr_inodes=151603,mode=700,uid=1000,gid=1000,inode64

            """.trimIndent().reader()
        val mountInfo = MountInfoParser.parseRecordSequence(input).toList()
        assertEquals(28, mountInfo.size)
    }

    @Test
    fun canParseAndroid9MountInfo() {
        val input =
            """
                1325 1324 179:26 / / ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1326 1325 0:15 / /dev rw,nosuid,relatime master:2 - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755
                1327 1326 0:17 / /dev/pts rw,relatime master:3 - devpts devpts rw,seclabel,mode=600,ptmxmode=000
                1328 1326 0:16 / /dev/cg2_bpf rw,nosuid,nodev,noexec,relatime master:8 - cgroup2 none rw
                1329 1326 0:21 / /dev/cpuctl rw,nosuid,nodev,noexec,relatime master:13 - cgroup none rw,cpu
                1330 1326 0:24 / /dev/cpuset rw,nosuid,nodev,noexec,relatime master:15 - cgroup none rw,cpuset,noprefix,release_agent=/sbin/cpuset_release_agent
                1331 1326 0:25 / /dev/stune rw,nosuid,nodev,noexec,relatime master:16 - cgroup none rw,schedtune
                1332 1326 0:28 / /dev/usb-ffs/adb rw,relatime master:12 - functionfs adb rw
                1333 1325 0:4 / /proc rw,relatime master:4 - proc proc rw,gid=3009,hidepid=2
                1334 1325 0:18 / /sys rw,relatime master:5 - sysfs sysfs rw,seclabel
                1335 1334 0:14 / /sys/fs/selinux rw,relatime master:6 - selinuxfs selinuxfs rw
                1336 1334 0:6 / /sys/kernel/debug rw,relatime master:17 - debugfs debugfs rw,seclabel
                1337 1336 0:8 / /sys/kernel/debug/tracing rw,relatime master:21 - tracefs tracefs rw,seclabel
                1338 1334 0:26 / /sys/fs/bpf rw,nosuid,nodev,noexec,relatime master:19 - bpf bpf rw
                1339 1334 0:27 / /sys/fs/pstore rw,nosuid,nodev,noexec,relatime master:20 - pstore pstore rw,seclabel
                1340 1325 0:19 / /mnt rw,nosuid,nodev,noexec,relatime master:7 - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755,gid=1000
                1341 1340 179:27 / /mnt/vendor/persist rw,nosuid,nodev,noatime master:23 - ext4 /dev/block/bootdevice/by-name/persist rw,seclabel,data=ordered
                1342 1340 0:29 / /mnt/runtime/default/emulated rw,nosuid,nodev,noexec,noatime master:26 - sdcardfs /data/media rw,fsuid=1023,fsgid=1023,gid=1015,multiuser,mask=6,derive_gid,default_normal
                1343 1340 0:29 / /mnt/runtime/read/emulated rw,nosuid,nodev,noexec,noatime master:26 - sdcardfs /data/media rw,fsuid=1023,fsgid=1023,gid=9997,multiuser,mask=23,derive_gid,default_normal
                1344 1340 0:29 / /mnt/runtime/write/emulated rw,nosuid,nodev,noexec,noatime master:26 - sdcardfs /data/media rw,fsuid=1023,fsgid=1023,gid=9997,multiuser,mask=7,derive_gid,default_normal
                1345 1340 0:29 / /mnt/runtime/full/emulated rw,nosuid,nodev,noexec,noatime master:26 - sdcardfs /data/media rw,fsuid=1023,fsgid=1023,gid=9997,multiuser,mask=7,derive_gid,default_normal
                1346 1325 0:20 / /apex rw,nosuid,nodev,noexec,relatime - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755
                1347 1346 179:26 /system/apex/com.android.tzdata /apex/com.android.tzdata@290000000 ro,relatime master:1 - ext4 /dev/root ro,seclabel
                1348 1346 179:26 /system/apex/com.android.tzdata /apex/com.android.tzdata ro,relatime master:1 - ext4 /dev/root ro,seclabel
                1349 1346 179:26 /system/apex/com.android.runtime.release /apex/com.android.runtime@1 ro,relatime master:1 - ext4 /dev/root ro,seclabel
                1350 1346 179:26 /system/apex/com.android.runtime.release /apex/com.android.runtime ro,relatime master:1 - ext4 /dev/root ro,seclabel
                1351 1346 179:26 /system/apex/com.android.resolv /apex/com.android.resolv@290000000 ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1352 1346 179:26 /system/apex/com.android.resolv /apex/com.android.resolv ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1353 1346 179:26 /system/apex/com.android.media.swcodec /apex/com.android.media.swcodec@290000000 ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1354 1346 179:26 /system/apex/com.android.media.swcodec /apex/com.android.media.swcodec ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1355 1346 179:26 /system/apex/com.android.media /apex/com.android.media@290000000 ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1356 1346 179:26 /system/apex/com.android.media /apex/com.android.media ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1357 1346 179:26 /system/apex/com.android.conscrypt /apex/com.android.conscrypt@299900000 ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1358 1346 179:26 /system/apex/com.android.conscrypt /apex/com.android.conscrypt ro,nodev,relatime master:1 - ext4 /dev/root ro,seclabel
                1359 1325 0:22 / /sbin rw,relatime master:9 - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755
                1360 1359 259:17 / /sbin/.magisk/mirror/data rw,relatime master:10 - ext4 /sbin/.magisk/block/data rw,seclabel,noauto_da_alloc,data=ordered
                1361 1359 179:26 / /sbin/.magisk/mirror/system_root ro,relatime master:11 - ext4 /sbin/.magisk/block/system_root ro,seclabel
                1362 1359 259:17 /adb/modules /sbin/.magisk/modules rw,relatime master:10 - ext4 /sbin/.magisk/block/data rw,seclabel,noauto_da_alloc,data=ordered
                1363 1325 0:23 / /acct rw,nosuid,nodev,noexec,relatime master:14 - cgroup none rw,cpuacct
                1364 1325 0:13 / /config rw,nosuid,nodev,noexec,relatime master:18 - configfs none rw
                1365 1325 259:17 / /data rw,nosuid,nodev,noatime master:22 - ext4 /dev/block/bootdevice/by-name/userdata rw,seclabel,noauto_da_alloc,data=ordered
                1366 1325 179:13 / /system/vendor/dsp ro,nosuid,nodev,relatime master:24 - ext4 /dev/block/bootdevice/by-name/dsp ro,seclabel,data=ordered
                1367 1325 179:2 / /system/vendor/firmware_mnt ro,relatime master:25 - vfat /dev/block/bootdevice/by-name/modem_b ro,context=u:object_r:firmware_file:s0,uid=1000,gid=1000,fmask=0337,dmask=0227,codepage=437,iocharset=utf8,shortname=lower,errors=remount-ro
                1368 1325 0:19 /runtime/write /storage rw,nosuid,nodev,noexec,relatime master:7 - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755,gid=1000
                1369 1368 0:29 / /storage/emulated rw,nosuid,nodev,noexec,noatime master:26 - sdcardfs /data/media rw,fsuid=1023,fsgid=1023,gid=9997,multiuser,mask=7,derive_gid,default_normal
                1370 1368 0:19 /user/0 /storage/self rw,nosuid,nodev,noexec,relatime master:7 - tmpfs tmpfs rw,seclabel,size=1818972k,nr_inodes=454743,mode=755,gid=1000
                1662 1340 179:65 / /mnt/media_rw/E1D5-160B rw,nosuid,nodev,noexec,noatime master:27 - vfat /dev/block/vold/public:179,65 rw,dirsync,uid=1023,gid=1023,fmask=0007,dmask=0007,allow_utime=0020,codepage=437,iocharset=utf8,shortname=mixed,utf8,errors=remount-ro
                1704 1340 0:30 / /mnt/runtime/default/E1D5-160B rw,nosuid,nodev,noexec,noatime master:28 - sdcardfs /mnt/media_rw/E1D5-160B rw,fsuid=1023,fsgid=1023,gid=1015,mask=6
                1757 1340 0:30 / /mnt/runtime/read/E1D5-160B rw,nosuid,nodev,noexec,noatime master:28 - sdcardfs /mnt/media_rw/E1D5-160B rw,fsuid=1023,fsgid=1023,gid=9997,mask=18
                1803 1368 0:30 / /storage/E1D5-160B rw,nosuid,nodev,noexec,noatime master:28 - sdcardfs /mnt/media_rw/E1D5-160B rw,fsuid=1023,fsgid=1023,gid=9997,mask=18
                1802 1340 0:30 / /mnt/runtime/write/E1D5-160B rw,nosuid,nodev,noexec,noatime master:28 - sdcardfs /mnt/media_rw/E1D5-160B rw,fsuid=1023,fsgid=1023,gid=9997,mask=18
                1895 1340 0:30 / /mnt/runtime/full/E1D5-160B rw,nosuid,nodev,noexec,noatime master:28 - sdcardfs /mnt/media_rw/E1D5-160B rw,fsuid=1023,fsgid=1023,gid=9997,mask=7

            """.trimIndent().reader()
        val mountInfo = MountInfoParser.parseRecordSequence(input).toList()
        assertEquals(52, mountInfo.size)
    }
}
