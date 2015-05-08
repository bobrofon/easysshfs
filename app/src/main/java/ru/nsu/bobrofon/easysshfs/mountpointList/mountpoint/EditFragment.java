package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import ru.nsu.bobrofon.easysshfs.DrawerStatus;
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.R;

public class EditFragment extends Fragment {
	private static final String MOUNT_POINT_ID = "MOUNT_POINT_ID";

	private int mMountPointId;
	private DrawerStatus mDrawerStatus;

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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);

		// Inflate the layout for this fragment
		View selfView = inflater.inflate(R.layout.fragment_edit, container, false);

		return selfView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (mDrawerStatus == null || !mDrawerStatus.isDrawerOpen()) {
			inflater.inflate(R.menu.edit, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_save) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((EasySSHFSActivity) activity).onSectionAttached(R.string.mount_point_title);
	}
}
