// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.annotation.TargetApi
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Set of deprecated android APIs used in EasySSHFS.
 * Get rid of this as soon as possible.
 */
object DeprecatedApi {
    object Environment {
        @TargetApi(Build.VERSION_CODES.P)
        @RequiresApi(Build.VERSION_CODES.BASE)
        fun getExternalStorageDirectory(): File {
            @Suppress("DEPRECATION")
            return android.os.Environment.getExternalStorageDirectory()
        }
    }
}
