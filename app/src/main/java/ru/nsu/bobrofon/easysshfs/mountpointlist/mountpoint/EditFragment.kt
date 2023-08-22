// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.annotation.RequiresApi
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle

import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.databinding.FragmentEditBinding
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initLocalDirSelector(selector: View) {
        selector.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectLocalDir()
            }
        }
        selector.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
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
            if (!drawerStatus.isDrawerOpen) {
                menuInflater.inflate(R.menu.edit, menu)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appActivity?.onSectionAttached(R.string.mount_point_title)
    }

    private fun showToast(message: String) {
        EasySSHFSActivity.showToast(message, context)
    }

    private val localDirPicker =
        registerForActivityResult(OpenDocumentTree()) { uri: Uri? ->
            uri?.let { setLocalPath(it) }
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun selectLocalDir() {
        localDirPicker.launch(/* starting location */ null)
    }

    private val identityFilePicker =
        registerForActivityResult(object : GetContent() {
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
        // Most of the time users select some directory, they are actually trying to select a path
        // on one of the bind-mounted runtime directories (like "/mnt/runtime/[default|write|read|...]/"),
        // because they just want to access the remote filesystem from most of their Android applications.
        // So let's replace the path they selected with the path in "/mnt/runtime/default" directory.
        // This is not necessary that a user would want us to do, but he can edit this path manually
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
        private const val MOUNT_POINT_ID = "MOUNT_POINT_ID"

        fun newInstance(id: Int): EditFragment {
            val fragment = EditFragment()
            val args = Bundle()
            args.putInt(MOUNT_POINT_ID, id)
            fragment.arguments = args
            return fragment
        }

        private fun sdcard(): String = when {
            // TODO(bobrofon): substitute an actual active user id instead of hardcoding '0'
            hasRuntimePermissions -> {
                "/mnt/runtime/default/emulated/0"
            }

            isMultiUserEnvironment -> {
                "/data/media/0"
            }

            else -> {
                "/mnt/sdcard"
            }
        }

        private fun toUnderlyingDefaultStorage(storagePath: String): String {
            return if (hasRuntimePermissions) {
                storagePath.replaceFirst(Regex("^/storage/"), "/mnt/runtime/default/")
            } else {
                storagePath
            }
        }

        private val isMultiUserEnvironment: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 // Android 4.2

        private val hasRuntimePermissions: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M // Android 6.0
    }
} // Required empty public constructor
