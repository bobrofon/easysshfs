package ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

import com.topjohnwu.superuser.Shell

import java.util.Locale

import ru.nsu.bobrofon.easysshfs.DrawerStatus
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.mountpoint_list.MountPointsList

import android.app.Activity.RESULT_OK
import android.content.Context

class EditFragment : Fragment() {

    private var mMountPointId: Int = 0
    private var mDrawerStatus: DrawerStatus? = null
    private var mSelf: MountPoint? = null

    private var mName: TextView? = null
    private var mAuto: CheckBox? = null
    private var mUsername: TextView? = null
    private var mHost: TextView? = null
    private var mPort: TextView? = null
    private var mPassword: TextView? = null
    private var mStorePassword: CheckBox? = null
    private var mIdentityFile: TextView? = null
    private var mRemotePath: TextView? = null
    private var mLocalPath: TextView? = null
    private var mForcePermissions: CheckBox? = null
    private var mOptions: TextView? = null

    private val shell: Shell?
        get() = (activity as EasySSHFSActivity).shell

    fun setDrawerStatus(drawerStatus: DrawerStatus) {
        mDrawerStatus = drawerStatus
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mMountPointId = arguments!!.getInt(MOUNT_POINT_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        val selfView = inflater.inflate(R.layout.fragment_edit, container, false)
        val activity = activity ?: return selfView

        val worker = MountPointsList.getIntent(activity)

        if (worker.mountPoints.size > mMountPointId) {
            mSelf = worker.mountPoints[mMountPointId]
        } else {
            mSelf = MountPoint()
            mSelf!!.rootDir = activity.filesDir.path
            mSelf!!.localPath = sdcard() + "/mnt"
        }

        mName = selfView.findViewById(R.id.mount_point_name)
        mAuto = selfView.findViewById(R.id.automount)
        mUsername = selfView.findViewById(R.id.username)
        mHost = selfView.findViewById(R.id.host)
        mPort = selfView.findViewById(R.id.port)
        mPassword = selfView.findViewById(R.id.password)
        mStorePassword = selfView.findViewById(R.id.store_password)
        mRemotePath = selfView.findViewById(R.id.remote_path)
        mLocalPath = selfView.findViewById(R.id.local_path)
        mForcePermissions = selfView.findViewById(R.id.force_permissions)
        mOptions = selfView.findViewById(R.id.sshfs_options)
        val selectLocalDir = selfView.findViewById<Button>(R.id.select_dir)
        selectLocalDir.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectLocalDir()
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            selectLocalDir.isEnabled = false
        }
        mIdentityFile = selfView.findViewById(R.id.identity_file)
        val selectIdentityFile = selfView.findViewById<Button>(R.id.identity_file_select)
        selectIdentityFile.setOnClickListener { selectIdentityFile() }

        mName!!.text = mSelf!!.visiblePointName
        mAuto!!.isChecked = mSelf!!.autoMount
        mUsername!!.text = mSelf!!.userName
        mHost!!.text = mSelf!!.host
        mPort!!.text = String.format(Locale.getDefault(), "%d", mSelf!!.port)
        mPassword!!.text = mSelf!!.password
        mStorePassword!!.isChecked = mSelf!!.storePassword
        mRemotePath!!.text = mSelf!!.remotePath
        mLocalPath!!.text = mSelf!!.localPath
        mForcePermissions!!.isChecked = mSelf!!.forcePermissions
        mOptions!!.text = mSelf!!.options
        mIdentityFile!!.text = mSelf!!.identityFile

        return selfView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (mDrawerStatus == null || !mDrawerStatus!!.isDrawerOpen) {
            inflater!!.inflate(R.menu.edit, menu)
        }
    }

    private fun grabMountPoint(mountPoint: MountPoint) {
        mountPoint.pointName = mName!!.text.toString()
        mountPoint.autoMount = mAuto!!.isChecked
        mountPoint.userName = mUsername!!.text.toString()
        mountPoint.host = mHost!!.text.toString()
        mountPoint.setPort(mPort!!.text.toString())
        mountPoint.password = mPassword!!.text.toString()
        mountPoint.storePassword = mStorePassword!!.isChecked
        mountPoint.identityFile = mIdentityFile!!.text.toString()
        mountPoint.remotePath = mRemotePath!!.text.toString()
        mountPoint.localPath = mLocalPath!!.text.toString()
        mountPoint.forcePermissions = mForcePermissions!!.isChecked
        mountPoint.options = mOptions!!.text.toString()
        mountPoint.rootDir = activity!!.filesDir.path
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        val activity = activity ?: return false
        val self = mSelf ?: return false
        val context = context ?: return false
        val shell = shell ?: return false

        when (id) {
            R.id.action_save -> {
                grabMountPoint(mSelf!!)

                val worker = MountPointsList.getIntent(activity)

                if (!worker.mountPoints.contains(self)) {
                    worker.mountPoints.add(self)
                }
                worker.save(activity)
                showToast("saved")

                return true
            }
            R.id.action_delete -> {
                val worker = MountPointsList.getIntent(activity)
                worker.mountPoints.remove(self)
                worker.save(activity)
                showToast("deleted")

                return true
            }
            R.id.action_mount -> {
                val mountPoint = MountPoint()
                grabMountPoint(mountPoint)
                mountPoint.mount(shell, context)
            }
            R.id.action_umount -> {
                val mountPoint = MountPoint()
                grabMountPoint(mountPoint)
                mountPoint.umount(shell, context)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (activity as EasySSHFSActivity).onSectionAttached(R.string.mount_point_title)
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
        startActivityForResult(Intent.createChooser(intent, "Select IdentityFile"),
                PICK_IDENTITY_FILE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val context = context ?: return
        when (requestCode) {
            PICKDIR_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                val localUrl = data!!.data ?: return
                mLocalPath!!.text = FileUtil.getFullPathFromTreeUri(localUrl, context)
            }
            PICK_IDENTITY_FILE_CODE -> if (resultCode == RESULT_OK) {
                val localUrl = data!!.data ?: return
                val path = FileUtil.getPath(context, localUrl)
                replaceIdentityFile(path)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun replaceIdentityFile(path: String?) {
        if (path == null || path.isEmpty()) {
            return
        }
        var options = mOptions!!.text.toString()
        options = options.replace(",?IdentityFile=[^,]*,?".toRegex(), ",")
                .replace(",$|^,".toRegex(), "")
        mOptions!!.text = options
        mIdentityFile!!.text = path
    }

    companion object {
        private const val MOUNT_POINT_ID = "MOUNT_POINT_ID"
        private const val PICKDIR_REQUEST_CODE = 1
        private const val PICK_IDENTITY_FILE_CODE = 2

        fun newInstance(mountPointId: Int): EditFragment {
            val fragment = EditFragment()
            val args = Bundle()
            args.putInt(MOUNT_POINT_ID, mountPointId)
            fragment.arguments = args
            return fragment
        }

        fun sdcard(): String {
            return if (isMultiUserEnvironment) {
                "/data/media/0"
            } else {
                "/mnt/sdcard"
            }
        }

        private val isMultiUserEnvironment: Boolean
            get() = android.os.Build.VERSION.SDK_INT >= 17
    }
}// Required empty public constructor
