// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.Shell.Builder
import ru.nsu.bobrofon.easysshfs.log.AppLog
import java.io.File
import java.lang.ref.WeakReference

object ShellBuilder {
    private const val TAG = "ShellBuilder"

    private var sharedShell = WeakReference<Shell?>(null)

    @Synchronized
    fun sharedShell(): Shell {
        val oldShell = sharedShell.get()
        if (oldShell == null) {
            Log.i(TAG, "create new shared shell")
            val newShell = build()
            sharedShell = WeakReference(newShell)
            return newShell
        }
        return oldShell
    }


    class NsenterInitiallizer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            if (!shell.isRoot) {
                AppLog.instance().addMessage("root shell is not detected")
            }

            val nsenter = File(context.filesDir.path, "nsenter")
            // TODO: use Zygote namespace instead
            val cmd = "${nsenter.path} -m -t 1"

            val result =
                shell.newJob().add(cmd).to(mutableListOf<String>(), mutableListOf<String>()).exec()
            return if (result.isSuccess) {
                Log.i(
                    TAG,
                    "'$cmd' succeed: code=${result.code} stdout=${result.out} stderr=${result.err}"
                )
                super.onInit(context, shell)
            } else {
                Log.w(
                    TAG,
                    "'$cmd' failed: code=${result.code} stdout=${result.out} stderr=${result.err}"
                )
                val errorMessage = result.err.joinToString("\n")
                AppLog.instance().addMessage("nsenter failed: '$errorMessage'")
                true // Don't crash the app. The only useful thing we can do here is logging.
            }
        }
    }

    private fun build(): Shell {
        return Builder.create().setInitializers(NsenterInitiallizer::class.java).build()
    }
}
