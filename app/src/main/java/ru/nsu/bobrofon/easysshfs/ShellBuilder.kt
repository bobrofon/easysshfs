package ru.nsu.bobrofon.easysshfs

import com.topjohnwu.superuser.BusyBoxInstaller
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.Shell.Builder

object ShellBuilder {
    fun create(): Builder {
        return Builder.create()
            .setFlags(Shell.FLAG_MOUNT_MASTER)
            .setInitializers(BusyBoxInstaller::class::java.get())
    }
}
