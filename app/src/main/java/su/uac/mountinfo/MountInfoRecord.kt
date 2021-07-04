// SPDX-License-Identifier: MIT
package su.uac.mountinfo

import java.io.File
import java.io.Reader

data class DeviceId(val major: UInt, val minor: UInt)

data class FilesystemType(val type: String, val subtype: String?)

/**
 * https://www.kernel.org/doc/Documentation/filesystems/proc.txt
 * 3.5	/proc/<pid>/mountinfo - Information about mounts
 */
data class MountInfoRecord(
    val mountId: Int, // unique identifier of the mount (may be reused after umount)
    val parentId: Int, // ID of parent (or of self for the top of the mount tree)
    val stDev: DeviceId, // value of st_dev for files on filesystem
    val root: File, // root of the mount within the filesystem
    val mountPoint: File, // mount point relative to the process's root
    val mountOptions: Map<String, String?>, // per mount options
    val optionalFields: Map<String, String?>, // zero or more fields
    val filesystemType: FilesystemType, // name of filesystem
    val mountSource: String, // filesystem specific information or "none"
    val superOptions: Map<String, String?>, // per super block options
) {
    val isShared: Boolean get() = sharedPeerGroup != null
    val isSlave: Boolean get() = masterPeerGroup != null || propagateFromPeerGroup != null
    val isUnbindable: Boolean get() = optionalFields.contains("unbindable")

    val sharedPeerGroup: Int? get() = optionalFields["shared"]?.toInt()
    val masterPeerGroup: Int? get() = optionalFields["master"]?.toInt()
    val propagateFromPeerGroup: Int? get() = optionalFields["propagate_from"]?.toInt()

    val isMountReadOnly: Boolean get() = mountOptions.contains("ro")
    val isMountWritable: Boolean get() = mountOptions.contains("rw")
    val isSuperBlockReadOnly: Boolean get() = superOptions.contains("ro")
    val isSuperBlockWritable: Boolean get() = superOptions.contains("rw")
}

object MountInfoParser {
    fun parseRecordSequence(input: Reader): Sequence<MountInfoRecord> = sequence {
        input.useLines { lines ->
            yieldAll(lines.map { parseRecord(it) }.filterNotNull())
        }
    }

    fun parseRecord(line: CharSequence): MountInfoRecord? {
        val recordValues = recordPattern.matchEntire(line)?.groupValues ?: return null
        var groupIdx = 0
        val mountId = recordValues[++groupIdx].toIntOrNull() ?: return null
        val parentId = recordValues[++groupIdx].toIntOrNull() ?: return null
        val stDev = parseDeviceId(recordValues[++groupIdx]) ?: return null
        val root = File(recordValues[++groupIdx])
        val mountPoint = File(recordValues[++groupIdx])
        val mountOptions = parseMap(optionsPattern, recordValues[++groupIdx])
        val optionalFields = if (recordValues.size > 10) {
            parseMap(fieldsPattern, recordValues[++groupIdx])
        } else emptyMap()
        val filesystemType = parseFilesystem(recordValues[++groupIdx]) ?: return null
        val mountSource = recordValues[++groupIdx]
        val superOptions = parseMap(optionsPattern, recordValues[++groupIdx])

        return MountInfoRecord(
            mountId = mountId,
            parentId = parentId,
            stDev = stDev,
            root = root,
            mountPoint = mountPoint,
            mountOptions = mountOptions,
            optionalFields = optionalFields,
            filesystemType = filesystemType,
            mountSource = mountSource,
            superOptions = superOptions,
        )
    }

    private fun parseDeviceId(raw: CharSequence): DeviceId? {
        val values = deviceIdPattern.matchEntire(raw)?.groupValues ?: return null
        return DeviceId(
            major = values[1].toUIntOrNull() ?: return null,
            minor = values[2].toUIntOrNull() ?: return null,
        )
    }

    private fun parseMap(pattern: Regex, raw: CharSequence): Map<String, String?> {
        return pattern.findAll(raw).map {
            val values = it.groupValues
            Pair(values[1], values.getOrNull(2))
        }.toMap()
    }

    private fun parseFilesystem(raw: CharSequence): FilesystemType? {
        val values = filesystemPattern.matchEntire(raw)?.groupValues ?: return null
        return FilesystemType(
            type = values[1],
            subtype = values.getOrNull(2),
        )
    }

    private val recordPattern =
        Regex("""(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)(?:(?:\s+)(.+))?\s+-\s+(\S+)\s+(\S+)\s+(\S+)""")
    private val deviceIdPattern = Regex("""(\d+):(\d+)""")
    private val optionsPattern = Regex("""([^=,\s]+)(?:=([^,\s]+))?""")
    private val fieldsPattern = Regex("""([^\s:]+)(?::(\S+))?""")
    private val filesystemPattern = Regex("""([^\s.]+)(?:.(\S+))?""")
}
