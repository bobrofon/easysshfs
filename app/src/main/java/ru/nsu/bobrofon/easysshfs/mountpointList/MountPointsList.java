package ru.nsu.bobrofon.easysshfs.mountpointList;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

import ru.nsu.bobrofon.easysshfs.log.LogSingleton;
import ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint.MountPoint;

public class MountPointsList {
	private final static String TAG = "MOUNT_POINTS_LIST";
	private final static String STORAGE_FILE = "mountpoints";

	private List<MountPoint> mMountPoints;

	private static MountPointsList self;

	private MountPointsList() {
	}

	public List<MountPoint> getMountPoints() {
		return mMountPoints;
	}

	public void autoMount(final Context context) {
		for (final MountPoint item : mMountPoints) {
			if (item.getAutoMount()/* && !item.isMounted()*/) {
				item.mount(context);
			}
		}
	}

	public void umount(final Context context) {
		for (final MountPoint item: mMountPoints) {
			item.umount(false, context);
		}
	}

	public void registerObserver(final MountPoint.Observer observer, final Context context) {
		for (final MountPoint item : mMountPoints) {
			item.registerObserver(observer, context);
		}
	}

	public void unregisterObserver(final MountPoint.Observer observer) {
		for (final MountPoint item : mMountPoints) {
			item.unregisterObserver(observer);
		}
	}

	private MountPointsList load(final Context context) {
		final SharedPreferences settings = context.getSharedPreferences(STORAGE_FILE, 0);
		try {
			JSONArray selfJson = new JSONArray(settings.getString(STORAGE_FILE, "[]"));

			mMountPoints = new LinkedList<>();
			for (int i = 0; i < selfJson.length(); ++i) {
				MountPoint mountPoint = new MountPoint();
				mountPoint.json(selfJson.getJSONObject(i));
				mMountPoints.add(mountPoint);
			}
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
			log(e.getMessage());
		}

		return this;
	}

	public void save(final Context context) {
		final SharedPreferences settings = context.getSharedPreferences(STORAGE_FILE, 0);

		JSONArray selJson = new JSONArray();
		for (MountPoint item : mMountPoints) {
			selJson.put(item.json());
		}

		SharedPreferences.Editor prefsEditor = settings.edit();
		prefsEditor.putString(STORAGE_FILE, selJson.toString()).apply();
	}

	private void log(final CharSequence message) {
		LogSingleton.getLogModel().addMessage(message);
	}

	public static MountPointsList getIntent(final Context context) {
		if (self == null) {
			self = new MountPointsList().load(context);
		}
		return self;
	}

}
