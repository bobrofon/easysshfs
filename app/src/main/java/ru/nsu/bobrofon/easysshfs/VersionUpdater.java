package ru.nsu.bobrofon.easysshfs;


import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VersionUpdater {
	public final Context mContext;

	public VersionUpdater(final Context context) {
		mContext = context;
	}

	public void update() {
		copyAssets("ssh", "ssh");
		copyAssets("sshfs", "sshfs");
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
				file.setExecutable(true);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
