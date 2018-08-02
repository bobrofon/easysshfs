package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.topjohnwu.superuser.Shell;

import java.util.Locale;

import ru.nsu.bobrofon.easysshfs.DrawerStatus;
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.R;
import ru.nsu.bobrofon.easysshfs.mountpointList.MountPointsList;

import static android.app.Activity.RESULT_OK;

public class EditFragment extends Fragment {
	private static final String MOUNT_POINT_ID = "MOUNT_POINT_ID";
	private static final int PICKDIR_REQUEST_CODE = 1;
	private static final int PICK_IDENTITY_FILE_CODE = 2;

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
	private TextView mIdentityFile;
	private TextView mRemotePath;
	private TextView mLocalPath;
	private CheckBox mForcePermissions;
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
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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

		mName = selfView.findViewById(R.id.mount_point_name);
		mAuto = selfView.findViewById(R.id.automount);
		mUsername = selfView.findViewById(R.id.username);
		mHost = selfView.findViewById(R.id.host);
		mPort = selfView.findViewById(R.id.port);
		mPassword = selfView.findViewById(R.id.password);
		mStorePassword = selfView.findViewById(R.id.store_password);
		mRemotePath = selfView.findViewById(R.id.remote_path);
		mLocalPath = selfView.findViewById(R.id.local_path);
		mForcePermissions = selfView.findViewById(R.id.force_permissions);
		mOptions = selfView.findViewById(R.id.sshfs_options);
		Button selectLocalDir = selfView.findViewById(R.id.select_dir);
		selectLocalDir.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					selectLocalDir();
				}
			}
		});
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			selectLocalDir.setEnabled(false);
		}
		mIdentityFile = selfView.findViewById(R.id.identity_file);
		final Button selectIdentityFile = selfView.findViewById(R.id.identity_file_select);
		selectIdentityFile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectIdentityFile();
			}
		});

		mName.setText(mSelf.getPointName());
		mAuto.setChecked(mSelf.getAutoMount());
		mUsername.setText(mSelf.getUserName());
		mHost.setText(mSelf.getHost());
		mPort.setText(String.format(Locale.getDefault(), "%d", mSelf.getPort()));
		mPassword.setText(mSelf.getPassword());
		mStorePassword.setChecked(mSelf.getStorePassword());
		mRemotePath.setText(mSelf.getRemotePath());
		mLocalPath.setText(mSelf.getLocalPath());
		mForcePermissions.setChecked(mSelf.getForcePermissions());
		mOptions.setText(mSelf.getOptions());
		mIdentityFile.setText(mSelf.getIdentityFile());

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
		mountPoint.setIdentityFile(mIdentityFile.getText().toString());
		mountPoint.setRemotePath(mRemotePath.getText().toString());
		mountPoint.setLocalPath(mLocalPath.getText().toString());
		mountPoint.setForcePermissions(mForcePermissions.isChecked());
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
			showToast("saved");

			return true;
		}
		else if (id == R.id.action_delete) {
			final MountPointsList worker
				= MountPointsList.getIntent(getActivity());
			worker.getMountPoints().remove(mSelf);
			worker.save(getActivity());
			showToast("deleted");

			return true;
		}
		else if (id == R.id.action_mount) {
			MountPoint mountPoint = new MountPoint();
			grabMountPoint(mountPoint);
			mountPoint.mount(true, getContext(), getShell());
		}
		else if (id == R.id.action_umount) {
			MountPoint mountPoint = new MountPoint();
			grabMountPoint(mountPoint);
			mountPoint.umount(true, getContext(), getShell());
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((EasySSHFSActivity) activity).onSectionAttached(R.string.mount_point_title);
	}

	private void showToast(final String message) {
		EasySSHFSActivity.showToast(message, getContext());
	}

	private Shell getShell() {
		return ((EasySSHFSActivity) getActivity()).getShell();
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private void selectLocalDir() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, PICKDIR_REQUEST_CODE);
	}

	private void selectIdentityFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(Intent.createChooser(intent, "Select IdentityFile"),
			PICK_IDENTITY_FILE_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case PICKDIR_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					final Uri localUrl = data.getData();
					mLocalPath.setText(FileUtil.getFullPathFromTreeUri(localUrl, getContext()));
				}
				break;
			case PICK_IDENTITY_FILE_CODE:
				if (resultCode == RESULT_OK) {
					final Uri localUrl = data.getData();
					final String path = FileUtil.getPath(getContext(), localUrl);
					replaceIdentityFile(path);
				}
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void replaceIdentityFile(final String path) {
		if (path == null || path.isEmpty()) {
			return;
		}
		String options = mOptions.getText().toString();
		options = options.replaceAll(",?IdentityFile=[^,]*,?", ",")
			.replaceAll(",$|^,", "");
		mOptions.setText(options);
		mIdentityFile.setText(path);
	}
}
