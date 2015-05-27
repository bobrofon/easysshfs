package ru.nsu.bobrofon.easysshfs.mountpointList;


import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ru.nsu.bobrofon.easysshfs.log.LogSingleton;
import ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint.MountPoint;

public class MountPointsList {
	public final static String TAG = "MOUNT_POINTS_LIST";
	public final static String STORAGE_FILE = "mountpoints";

	private List<MountPoint> mMountPoints;

	private static final MountPointsList self = new MountPointsList();

	public MountPointsList() {
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

	public void registerObserver(final MountPoint.Observer observer) {
		for(Iterator<MountPoint> i = mMountPoints.iterator(); i.hasNext(); ) {
			final MountPoint item = i.next();
			item.registerObserver(observer);
		}
	}

	public void unregisterObserver(final MountPoint.Observer observer) {
		for(Iterator<MountPoint> i = mMountPoints.iterator(); i.hasNext(); ) {
			final MountPoint item = i.next();
			item.unregisterObserver(observer);
		}
	}

	public MountPointsList load(final Context context) {
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

		for(Iterator<MountPoint> i = mMountPoints.iterator(); i.hasNext(); ) {
			final MountPoint item = i.next();
			item.init();
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
		LogSingleton.getLogModel().addMessage(message);
	}

	public static MountPointsList getIntent() {
		return self;
	}

}
