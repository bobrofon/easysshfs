package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.app.Activity.RESULT_OK
import android.content.Context
import android.net.Uri

import kotlinx.android.synthetic.main.fragment_edit.mount_point_name as name
import kotlinx.android.synthetic.main.fragment_edit.automount as auto
import kotlinx.android.synthetic.main.fragment_edit.username
import kotlinx.android.synthetic.main.fragment_edit.host as hostname
import kotlinx.android.synthetic.main.fragment_edit.port
import kotlinx.android.synthetic.main.fragment_edit.password
import kotlinx.android.synthetic.main.fragment_edit.store_password as storePassword
import kotlinx.android.synthetic.main.fragment_edit.remote_path as remotePath
import kotlinx.android.synthetic.main.fragment_edit.local_path as localPath
import kotlinx.android.synthetic.main.fragment_edit.force_permissions as forcePermissions
import kotlinx.android.synthetic.main.fragment_edit.sshfs_options as options
import kotlinx.android.synthetic.main.fragment_edit.select_dir as selectLocalDir
import kotlinx.android.synthetic.main.fragment_edit.identity_file as identityFile
import kotlinx.android.synthetic.main.fragment_edit.identity_file_select as selectIdentityFile

import ru.nsu.bobrofon.easysshfs.DrawerStatus
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList

class EditFragment : EasySSHFSFragment() {

    private var mountPointId: Int = 0
    private lateinit var drawerStatus: DrawerStatus
    private lateinit var self: MountPoint

    fun setDrawerStatus(status: DrawerStatus) {
        drawerStatus = status
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mountPointId = arguments?.getInt(MOUNT_POINT_ID)
            ?: throw IllegalStateException("$MOUNT_POINT_ID argument is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        val context = context!!

        val worker = MountPointsList.getIntent(context)

        self = if (worker.mountPoints.size > mountPointId) {
            worker.mountPoints[mountPointId]
        } else {
            MountPoint().apply {
                rootDir = context.filesDir.path
                localPath = sdcard() + "/mnt"
            }
        }

        return inflater.inflate(R.layout.fragment_edit, container, false)
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
    }

    private fun initLocalDirSelector(selector: View) {
        selector.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectLocalDir()
            }
        }
        selector.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!drawerStatus.isDrawerOpen) {
            inflater?.inflate(R.menu.edit, menu)
        }
    }

    private fun grabMountPoint(mountPoint: MountPoint) {
        val context = context!!

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
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
        else -> super.onOptionsItemSelected(item)
    }

    private fun saveAction() {
        val context = context!!

        grabMountPoint(self)

        val worker = MountPointsList.getIntent(context)
        if (!worker.mountPoints.contains(self)) {
            worker.mountPoints.add(self)
        }
        worker.save(context)
        showToast("saved")
    }

    private fun deleteAction() {
        val context = context!!

        val worker = MountPointsList.getIntent(context)
        worker.mountPoints.remove(self)
        worker.save(context)
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appActivity?.onSectionAttached(R.string.mount_point_title)
    }

    private fun showToast(message: String) {
        EasySSHFSActivity.showToast(message, context)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun selectLocalDir() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, PICKDIR_REQUEST_CODE)
    }

    private fun selectIdentityFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(
            Intent.createChooser(intent, "Select IdentityFile"),
            PICK_IDENTITY_FILE_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICKDIR_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                data?.data?.let { setLocalPath(it) }
            }
            PICK_IDENTITY_FILE_CODE -> if (resultCode == RESULT_OK) {
                data?.data?.let { setIdentityFile(it) }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setLocalPath(uri: Uri) {
        val context = context!!

        val path = FileUtil.getFullPathFromTreeUri(uri, context)
        localPath.setText(path)
    }

    private fun setIdentityFile(uri: Uri) {
        val context = context!!

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
        private const val PICKDIR_REQUEST_CODE = 1
        private const val PICK_IDENTITY_FILE_CODE = 2

        fun newInstance(id: Int): EditFragment {
            val fragment = EditFragment()
            val args = Bundle()
            args.putInt(MOUNT_POINT_ID, id)
            fragment.arguments = args
            return fragment
        }

        fun sdcard(): String = if (isMultiUserEnvironment) {
            "/data/media/0"
        } else {
            "/mnt/sdcard"
        }

        private val isMultiUserEnvironment: Boolean
            get() = Build.VERSION.SDK_INT >= 17
    }
} // Required empty public constructor
