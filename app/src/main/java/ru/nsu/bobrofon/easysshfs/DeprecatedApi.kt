// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.annotation.TargetApi
import android.os.Build
import java.io.File

/**
 * Set of deprecated android APIs used in EasySSHFS.
 * Get rid of this as soon as possible.
 */
object DeprecatedApi {
    object Environment {
        @TargetApi(Build.VERSION_CODES.P)
        fun getExternalStorageDirectory(): File {
            return android.os.Environment.getExternalStorageDirectory()
        }
    }
}
