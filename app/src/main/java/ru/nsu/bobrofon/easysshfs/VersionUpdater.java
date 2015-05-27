package ru.nsu.bobrofon.easysshfs;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.nsu.bobrofon.easysshfs.log.LogSingleton;
import ru.nsu.bobrofon.easysshfs.mountpointList.MountPointsList;
import ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint.MountPoint;

public class VersionUpdater {
	public final Context mContext;

	public VersionUpdater(final Context context) {
		mContext = context;
	}

	public void update() {
		copyAssets("ssh", "ssh");
		copyAssets("sshfs", "sshfs");

		int currentVersion = BuildConfig.VERSION_CODE;
		final SharedPreferences settings = mContext.getSharedPreferences("sshfs", 0);
		int lastVersion = settings.getInt("version", 0);

		if (lastVersion < 9) {
			update02to03();
		}

		SharedPreferences.Editor prefsEditor = settings.edit();
		prefsEditor.putInt("version", currentVersion);
		prefsEditor.commit();
	}

	public void update02to03() {
		final SharedPreferences settings = mContext.getSharedPreferences("sshfs_cmd_global", 0);
		if (!settings.contains("host")) {
			return;
		}

		final MountPoint mountPoint = new MountPoint();
		mountPoint.setRootDir(settings.getString("root_dir", mContext.getFilesDir().getPath()));
		mountPoint.setOptions(settings.getString("sshfs_opts",
			"password_stdin,UserKnownHostsFile=/dev/null,StrictHostKeyChecking=no"
				+ ",rw,dirsync,nosuid,nodev,noexec,umask=0702,allow_other"));
		mountPoint.setUserName(settings.getString("username", ""));
		mountPoint.setHost(settings.getString("host", ""));
		mountPoint.setPort(Integer.toString(settings.getInt("port", 22)));
		mountPoint.setLocalPath(settings.getString("local_dir",
			Environment.getExternalStorageDirectory().getPath() + "/mnt"));
		mountPoint.setRemotePath(settings.getString("remote_dir", ""));

		final MountPointsList list = MountPointsList.getIntent(mContext);
		list.getMountPoints().add(mountPoint);
		list.save(mContext);
	}

	private void copyAssets(final String assetPath, final String localPath) {
		try {
			final String home = mContext.getFilesDir().getPath();
			File file = new File(home + "/" + localPath);
			if(!file.exists()) {
				InputStream in = mContext.getAssets().open(assetPath);
				FileOutputStream out = new FileOutputStream(home + "/" + localPath);
				int read;
				byte[] buffer = new byte[4096];
				while ((read = in.read(buffer)) > 0) {
					out.write(buffer, 0, read);
				}
				out.close();
				in.close();

				file = new File(home + "/" + localPath);
				if (!file.setExecutable(true)) {
					LogSingleton.getLogModel().addMessage("Can't set executable bit on " + localPath);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
