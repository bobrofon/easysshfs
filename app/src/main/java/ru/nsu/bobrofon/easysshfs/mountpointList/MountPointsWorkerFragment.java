package ru.nsu.bobrofon.easysshfs.mountpointList;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ru.nsu.bobrofon.easysshfs.R;
import ru.nsu.bobrofon.easysshfs.log.LogFragment;
import ru.nsu.bobrofon.easysshfs.log.LogModel;
import ru.nsu.bobrofon.easysshfs.log.LogWorkerFragment;
import ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint.MountPoint;

/**
 * A simple {@link Fragment} subclass.
 */
public class MountPointsWorkerFragment extends Fragment {
	public final static String TAG = "MOUNT_POINTS_WORKER_FRAGMENT";
	public final static String STORAGE_FILE = "mountpoints";

	private LogModel mLog;
	private List<MountPoint> mMountPoints;

	public MountPointsWorkerFragment() {
	}

	public List<MountPoint> getMountPoints() {
		return mMountPoints;
	}

	public void autoMount() {
		for(Iterator<MountPoint> i = mMountPoints.iterator(); i.hasNext(); ) {
			final MountPoint item = i.next();
			if (item.getAutoMount() && !item.isMounted()) {
				item.mount();
			}
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public MountPointsWorkerFragment load(final Context context) {
		if (mMountPoints != null) {
			return this;
		}

		mMountPoints = new LinkedList<MountPoint>();
		try {
			FileInputStream fis = context.openFileInput(STORAGE_FILE);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mMountPoints = (LinkedList<MountPoint>) ois.readObject();
			ois.close();
		} catch (Exception ex) {
			log(ex.getMessage());
		}

		return this;
	}

	public void save(final Context context) {
		try {
			FileOutputStream fos = context.openFileOutput(STORAGE_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mMountPoints);
			oos.close();
		} catch (Exception ex) {
			log(ex.getMessage());
		}
	}

	private void log(final CharSequence message) {
		if (mLog != null) {
			mLog.addMessage(message);
		}
	}

	public static MountPointsWorkerFragment getFragment(final FragmentManager fragmentManager) {
		MountPointsWorkerFragment retainedWorkerFragment =
			(MountPointsWorkerFragment) fragmentManager.findFragmentByTag(TAG);

		if (retainedWorkerFragment == null) {
			retainedWorkerFragment = new MountPointsWorkerFragment();
			retainedWorkerFragment.mLog = LogWorkerFragment.getLogModelByTag(fragmentManager,
				LogFragment.TAG_WORKER);

			fragmentManager.beginTransaction()
				.add(retainedWorkerFragment, TAG)
				.commit();

		}

		return retainedWorkerFragment;
	}

}
