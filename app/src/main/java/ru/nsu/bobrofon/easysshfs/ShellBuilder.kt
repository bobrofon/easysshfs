package ru.nsu.bobrofon.easysshfs

import android.util.Log
import com.topjohnwu.superuser.BusyBoxInstaller
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.Shell.Builder
import ru.nsu.bobrofon.easysshfs.log.AppLog
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

    private fun build(): Shell {
        val builder = Builder.create()
            .setInitializers(BusyBoxInstaller::class::java.get())

        return if (isMagiskV27x()) {
            // TODO: remove logging after the following release (or maybe a few releases)
            AppLog.instance().addMessage("Magisk SU v27+ detected")
            // '--master-mount' actually spawns shell in an isolated mount namespace
            // try to run shell in an init process namespace instead
            // TODO: use Zygote namespace instead
            builder.build("su", "-t", "1")
        } else {
            // assume that 'su' implementation supports '--master-mount' option
            // (SuperSu or Magisk until v27.0)
            builder.setFlags(Shell.FLAG_MOUNT_MASTER)
            builder.build()
        }
    }

    private fun isMagiskV27x(): Boolean {
        val sh = Builder.create()
            .setInitializers(BusyBoxInstaller::class::java.get())
            .setFlags(Shell.FLAG_NON_ROOT_SHELL)
            .build()

        // The result should be something like this:
        //   $ su -v 2>/dev/null
        //   27.0:MAGISKSU
        //   $ echo $?
        //   0
        val displayVersionCmd = "su -v"
        val verResult = sh.newJob().add(displayVersionCmd).to(arrayListOf()).exec()
        if (!verResult.isSuccess) {
            Log.d(TAG, "failed to exec '$displayVersionCmd' (exit with ${verResult.code})")
            return false
        }
        if (verResult.out.isEmpty()) {
            Log.d(TAG, "'$displayVersionCmd' output is empty")
            return false
        }

        // 27.0:MAGISKSU
        val match = Regex("(\\d+)\\.?.*:MAGISKSU").matchEntire(verResult.out[0])
        if (match == null) {
            Log.d(TAG, "Magisk SU version signature is not found in ${verResult.out}")
            return false
        }
        Log.d(TAG, "detected Magisk SU version signature in ${verResult.out}")
        val majorVerString = match.groupValues[1] // 0 is the whole match
        val majorVer = try {
            majorVerString.toInt()
        } catch (e: NumberFormatException) {
            AppLog.instance().addMessage("failed to parse Magisk SU version '$majorVerString'")
            Log.w(TAG, "failed to parse '$majorVerString'", e)
            return false
        }

        Log.d(TAG, "detected Magisk SU version $majorVer")
        return majorVer >= 27
    }
}
