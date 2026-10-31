// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.databinding.FragmentEditBinding
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import java.io.File
import java.io.IOException

class EditFragment : EasySSHFSFragment() {

    private var mountPointId: Int = 0
    private lateinit var mountPointsList: MountPointsList
    private lateinit var self: MountPoint

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!
    private val name get() = binding.mountPointName
    private val auto get() = binding.automount
    private val username get() = binding.username
    private val hostname get() = binding.host
    private val port get() = binding.port
    private val password get() = binding.password
    private val storePassword get() = binding.storePassword
    private val remotePath get() = binding.remotePath
    private val localPath get() = binding.localPath
    private val forcePermissions get() = binding.forcePermissions
    private val options get() = binding.sshfsOptions
    private val identityFile get() = binding.identityFile
    private val selectLocalDir get() = binding.selectDir
    private val selectIdentityFile get() = binding.identityFileSelect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mountPointId = arguments?.getInt(MOUNT_POINT_ID)
            ?: throw IllegalStateException("$MOUNT_POINT_ID argument is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val context = requireContext()

        mountPointsList = MountPointsList.instance(context)

        self = if (mountPointsList.mountPoints.size > mountPointId) {
            mountPointsList.mountPoints[mountPointId]
        } else {
            MountPoint().apply {
                rootDir = context.filesDir.path
                localPath = sdcard() + "/mnt"
            }
        }

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLocalDirSelector(selectLocalDir)
        selectIdentityFile.setOnClickListener { selectIdentityFile() }

        name.setText(self.visiblePointName)
        auto.isChecked = self.autoMount
        username.setText(self.userName)
        hostname.setText(self.host)
        port.setText(self.port.toString())
        password.setText(self.password)
        storePassword.isChecked = self.storePassword
        remotePath.setText(self.remotePath)
        localPath.setText(self.localPath)
        forcePermissions.isChecked = self.forcePermissions
        options.setText(self.options)
        identityFile.setText(self.identityFile)

        auto.setOnCheckedChangeListener { _, isAutoMountEnabled ->
            val optionList = options.text.split(',').toMutableList()

            if (isAutoMountEnabled) {
                if (!optionList.contains("reconnect")) {
                    optionList.add("reconnect")
                }
            } else {
                if (optionList.last() == "reconnect") {
                    optionList.removeLastOrNull()
                }
            }

            options.setText(optionList.joinToString(","))
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initLocalDirSelector(selector: View) {
        selector.setOnClickListener {
            selectLocalDir()
        }
        selector.isEnabled = true
    }

    private fun grabMountPoint(mountPoint: MountPoint) {
        val context = requireContext()

        mountPoint.pointName = name.text.toString()
        mountPoint.autoMount = auto.isChecked
        mountPoint.userName = username.text.toString()
        mountPoint.host = hostname.text.toString()
        mountPoint.setPort(port.text.toString())
        mountPoint.password = password.text.toString()
        mountPoint.storePassword = storePassword.isChecked
        mountPoint.identityFile = identityFile.text.toString()
        mountPoint.remotePath = remotePath.text.toString()
        mountPoint.localPath = localPath.text.toString()
        mountPoint.forcePermissions = forcePermissions.isChecked
        mountPoint.options = options.text.toString()
        mountPoint.rootDir = context.filesDir.path
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.edit, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.action_save -> {
                saveAction()
                true
            }

            R.id.action_delete -> {
                deleteAction()
                true
            }

            R.id.action_mount -> {
                mountAction()
                true
            }

            R.id.action_umount -> {
                umountAction()
                true
            }

            else -> false
        }
    }

    private fun saveAction() {
        val context = requireContext()

        grabMountPoint(self)

        if (!mountPointsList.mountPoints.contains(self)) {
            mountPointsList.mountPoints.add(self)
        }
        mountPointsList.save(context)
        showToast("saved")
    }

    private fun deleteAction() {
        val context = requireContext()

        mountPointsList.mountPoints.remove(self)
        mountPointsList.save(context)
        showToast("deleted")
    }

    private fun mountAction() {
        val shell = shell!!

        val mountPoint = MountPoint()
        grabMountPoint(mountPoint)
        mountPoint.mount(shell, context)
    }

    private fun umountAction() {
        val shell = shell!!

        val mountPoint = MountPoint()
        grabMountPoint(mountPoint)
        mountPoint.umount(shell, context)
    }

    private fun showToast(message: String) {
        EasySSHFSActivity.showToast(message, context)
    }

    private val localDirPicker = registerForActivityResult(OpenDocumentTree()) { uri: Uri? ->
        uri?.let { setLocalPath(it) }
    }

    private fun selectLocalDir() {
        localDirPicker.launch(/* starting location */ null)
    }

    private val identityFilePicker = registerForActivityResult(object : GetContent() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            return Intent.createChooser(intent, "Select IdentityFile")
        }
    }) { uri: Uri? ->
        uri?.let { setIdentityFile(it) }
    }

    private fun selectIdentityFile() {
        identityFilePicker.launch("*/*")
    }

    private fun setLocalPath(uri: Uri) {
        val context = requireContext()

        val path = FileUtil.getFullPathFromTreeUri(uri, context)
        // Most of the time users select some directory, they want to access the remote filesystem
        // from most of their Android applications. "/storage/..." is a view (FUSE, sdcardfs, or
        // bind mount) built on top of the real storage, and a mount made on it is either not
        // possible or not visible to other apps. So let's replace the path they selected with the
        // path in the underlying storage (see toUnderlyingDefaultStorage()).
        // This is not necessarily what a user wants, but they can edit this path manually
        // in case we are wrong.
        val runtimeStoragePath = toUnderlyingDefaultStorage(path)
        localPath.setText(runtimeStoragePath)
    }

    private fun setIdentityFile(uri: Uri) {
        val context = requireContext()

        val path = FileUtil.getPath(uri, context) ?: return
        replaceIdentityFile(path)
    }

    private fun replaceIdentityFile(path: String) {
        var optionString = options.text.toString()
        optionString = optionString.replace(",?IdentityFile=[^,]*,?".toRegex(), ",")
            .replace(",$|^,".toRegex(), "")
        options.setText(optionString)
        identityFile.setText(path)
    }

    companion object {
        private const val TAG = "EditFragment"

        private const val MOUNT_POINT_ID = "MOUNT_POINT_ID"

        /**
         *  Range of UIDs allocated for a user.
         *  See https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/os/UserHandle.java#51
         */
        private const val PER_USER_RANGE = 100000

        /**
         * E.g. /storage/emulated/0/sshfs_mountpoint
         */
        private val STORAGE_PATH_REGEXP = Regex("^/storage/([^/]+)(/.*)?$")

        fun createArgs(id: Int): Bundle = Bundle().apply {
            putInt(MOUNT_POINT_ID, id)
        }

        /**
         * Calculates user id like the hidden UserHandle.getUserId does
         * See https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/os/UserHandle.java#319
         */
        private val currentUserId: Int
            get() = Process.myUid() / PER_USER_RANGE

        private fun sdcard(): String = when {
            hasRuntimePermissions -> {
                toUnderlyingDefaultStorage("/storage/emulated/$currentUserId")
            }

            else -> {
                "/data/media/$currentUserId"
            }
        }

        /**
         * Map "/storage/<volume>/<path>" to the directory "/storage/<volume>" is built on:
         *   - "/mnt/pass_through/<user>/<volume>/<path>" on Android 11+ with FUSE.
         *   - "/mnt/runtime/default/<volume>/<path>" on Android 6-10 or devices with sdcardfs without FUSE.
         */
        private fun toUnderlyingDefaultStorage(storagePath: String): String {
            if (!hasRuntimePermissions) {
                return storagePath
            }

            val match = STORAGE_PATH_REGEXP.matchEntire(storagePath) ?: return storagePath
            val volume = match.groupValues[1]
            val path = match.groupValues[2]

            val runtimeVolume = "/mnt/runtime/default/$volume"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // There is no FUSE pass-through before Android 11.
                return runtimeVolume + path
            }

            val mountPoints = readMountPoints()
            val passThroughVolume = "/mnt/pass_through/$currentUserId/$volume"
            if (passThroughVolume in mountPoints) {
                return passThroughVolume + path
            }

            if (runtimeVolume in mountPoints) {
                return runtimeVolume + path
            }

            return storagePath
        }

        /**
         * Mount points of the app's mount namespace.
         * The app has no access to init's mount namespace, but the storage mounts in the app
         * namespace are the same.
         */
        private fun readMountPoints(): Set<String> = try {
            File("/proc/self/mountinfo").useLines { lines ->
                // "<mount id> <parent id> <major:minor> <root> <mount point> <mount options> ..."
                lines.mapNotNull { line -> line.split(' ').getOrNull(4) }.toSet()
            }
        } catch (e: IOException) {
            Log.w(TAG, "Can't read mount points", e)
            emptySet()
        }

        private val hasRuntimePermissions: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M // Android 6.0
    }
} // Required empty public constructor
