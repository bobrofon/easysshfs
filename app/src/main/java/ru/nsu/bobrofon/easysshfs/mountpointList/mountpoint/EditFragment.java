package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import ru.nsu.bobrofon.easysshfs.DrawerStatus;
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.R;
import ru.nsu.bobrofon.easysshfs.mountpointList.MountPointsList;

public class EditFragment extends Fragment {
	private static final String MOUNT_POINT_ID = "MOUNT_POINT_ID";

	private int mMountPointId;
	private DrawerStatus mDrawerStatus;
	private MountPoint mSelf;

	private TextView mName;
	private CheckBox mAuto;
	private TextView mUsername;
	private TextView mHost;
	private TextView mPort;
	private TextView mPassword;
	private CheckBox mStorePassword;
	private TextView mRemotePath;
	private TextView mLocalPath;
	private TextView mOptions;

	public void setDrawerStatus(final DrawerStatus drawerStatus) {
		mDrawerStatus = drawerStatus;
	}

	public static EditFragment newInstance(final int mountPointId) {
		EditFragment fragment = new EditFragment();
		Bundle args = new Bundle();
		args.putInt(MOUNT_POINT_ID, mountPointId);
		fragment.setArguments(args);
		return fragment;
	}

	public EditFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mMountPointId = getArguments().getInt(MOUNT_POINT_ID);
		}
	}

	public static String sdcard()
	{
		if (isMultiUserEnvironment())
		{
			return "/data/media/0";
		} else
		{
			return "/mnt/sdcard";
		}
	}

	public static boolean isMultiUserEnvironment()
	{
		return android.os.Build.VERSION.SDK_INT >= 17;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);

		// Inflate the layout for this fragment
		final View selfView = inflater.inflate(R.layout.fragment_edit, container, false);

		final MountPointsList worker
			= MountPointsList.getIntent(getActivity());

		if (worker.getMountPoints().size() > mMountPointId) {
			mSelf = worker.getMountPoints().get(mMountPointId);
		}
		else {
			mSelf = new MountPoint();
			mSelf.setRootDir(getActivity().getFilesDir().getPath());
			mSelf.setLocalPath(sdcard() + "/mnt");
		}

		mName = (TextView)selfView.findViewById(R.id.mount_point_name);
		mAuto = (CheckBox)selfView.findViewById(R.id.automount);
		mUsername = (TextView)selfView.findViewById(R.id.username);
		mHost = (TextView)selfView.findViewById(R.id.host);
		mPort = (TextView)selfView.findViewById(R.id.port);
		mPassword = (TextView)selfView.findViewById(R.id.password);
		mStorePassword = (CheckBox)selfView.findViewById(R.id.store_password);
		mRemotePath = (TextView)selfView.findViewById(R.id.remote_path);
		mLocalPath = (TextView)selfView.findViewById(R.id.local_path);
		mOptions = (TextView)selfView.findViewById(R.id.sshfs_options);

		mName.setText(mSelf.getPointName());
		mAuto.setChecked(mSelf.getAutoMount());
		mUsername.setText(mSelf.getUserName());
		mHost.setText(mSelf.getHost());
		mPort.setText(Integer.toString(mSelf.getPort()));
		mPassword.setText(mSelf.getPassword());
		mStorePassword.setChecked(mSelf.getStorePassword());
		mRemotePath.setText(mSelf.getRemotePath());
		mLocalPath.setText(mSelf.getLocalPath());
		mOptions.setText(mSelf.getOptions());

		return selfView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (mDrawerStatus == null || !mDrawerStatus.isDrawerOpen()) {
			inflater.inflate(R.menu.edit, menu);
		}
	}

	private void grabMountPoint(final MountPoint mountPoint) {
		mountPoint.setPointName(mName.getText().toString());
		mountPoint.setAutoMount(mAuto.isChecked());
		mountPoint.setUserName(mUsername.getText().toString());
		mountPoint.setHost(mHost.getText().toString());
		mountPoint.setPort(mPort.getText().toString());
		mountPoint.setPassword(mPassword.getText().toString());
		mountPoint.setStorePassword(mStorePassword.isChecked());
		mountPoint.setRemotePath(mRemotePath.getText().toString());
		mountPoint.setLocalPath(mLocalPath.getText().toString());
		mountPoint.setOptions(mOptions.getText().toString());
		mountPoint.setRootDir(getActivity().getFilesDir().getPath());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_save) {
			grabMountPoint(mSelf);

			final MountPointsList worker = MountPointsList.getIntent(getActivity());

			if (!worker.getMountPoints().contains(mSelf)) {
				worker.getMountPoints().add(mSelf);
			}
			worker.save(getActivity());
			EasySSHFSActivity.showToast("saved");

			return true;
		}
		else if (id == R.id.action_delete) {
			final MountPointsList worker
				= MountPointsList.getIntent(getActivity());
			worker.getMountPoints().remove(mSelf);
			worker.save(getActivity());
			EasySSHFSActivity.showToast("deleted");

			return true;
		}
		else if (id == R.id.action_mount) {
			MountPoint mountPoint = new MountPoint();
			grabMountPoint(mountPoint);
			mountPoint.mount(true);
		}
		else if (id == R.id.action_umount) {
			MountPoint mountPoint = new MountPoint();
			grabMountPoint(mountPoint);
			mountPoint.umount(true);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((EasySSHFSActivity) activity).onSectionAttached(R.string.mount_point_title);
	}
}
