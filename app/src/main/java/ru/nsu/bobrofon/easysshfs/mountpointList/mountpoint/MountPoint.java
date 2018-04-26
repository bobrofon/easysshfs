package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;

import android.content.Context;
import android.database.Observable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.log.LogSingleton;

public class MountPoint {
	private static String TAG = "MOUNT_POINT";
	private static transient int commandCode = 0;

	private String mPointName;
	private boolean mAutoMount;
	private String mUserName;
	private String mHost;
	private int mPort;
	private String mPassword;
	private boolean mStorePassword;
	private String mRemotePath;
	private String mLocalPath;
	private String mOptions;
	private String mRootDir;

	private MountObservable mObservable = new MountObservable();
	private boolean mIsMounted = false;

	private final static String DEFAULT_OPTIONS =
		"password_stdin,"
		+ "UserKnownHostsFile=/dev/null,"
		+ "StrictHostKeyChecking=no,"
		+ "rw,"
		+ "dirsync,"
		+ "nosuid,"
		+ "nodev,"
		+ "noexec,"
		+ "umask=0,"
		+ "allow_other";

	public MountPoint() {
		mPointName = "";
		mAutoMount = false;
		mUserName = "";
		mHost = "";
		mPort = 22;
		mPassword = "";
		mStorePassword = false;
		mRemotePath = "";
		mLocalPath = "";
		mOptions = DEFAULT_OPTIONS;
		mRootDir = "";
	}

	public void setPointName(final String name) {
		mPointName = name;
	}

	public void setAutoMount(final boolean autoMount) {
		mAutoMount = autoMount;
	}

	public void setUserName(final String userName) {
		mUserName = userName;
	}

	public void setHost(final String host) {
		mHost = host;
	}

	public void setPort(final int port) {
		if (port < 0 || port > 65535) {
			logMessage("Port " + port + " is out of range [0; 65535]");
			return;
		}
		mPort = port;
	}

	public void setPort(final String port) {
		try {
			setPort(Integer.parseInt(port));
		}
		catch (final NumberFormatException e) {
			logMessage("'" + port + "' is invalid port value");
		}
	}

	public void setPassword(final String password) {
		mPassword = password;
	}

	public void setStorePassword(final boolean storePassword) {
		mStorePassword = storePassword;
	}

	public void setRemotePath(final String remotePath) {
		mRemotePath = remotePath;
	}

	public void setLocalPath(final String localPath) {
		mLocalPath = localPath;
	}

	public void setOptions(final String options) {
		mOptions = options;
	}

	public void setRootDir(final String rootDir) {
		mRootDir = rootDir;
	}

	public String getPointName() {
		if (!mPointName.isEmpty()) {
			return mPointName;
		}
		else {
			return mLocalPath;
		}
	}

	public boolean getAutoMount() {
		return mAutoMount;
	}

	public String getUserName() {
		return mUserName;
	}

	public String getHost() {
		return mHost;
	}

	public int getPort() {
		return mPort;
	}

	public String getPassword() {
		return mPassword;
	}

	public boolean getStorePassword() {
		return mStorePassword;
	}

	public String getRemotePath() {
		return mRemotePath;
	}

	public String getLocalPath() {
		return mLocalPath;
	}

	public String getOptions() {
		return mOptions;
	}

	public JSONObject json() {
		JSONObject selfJson = new JSONObject();
		try {
			selfJson.put("PointName", mPointName);
			selfJson.put("AutoMount", mAutoMount);
			selfJson.put("UserName", mUserName);
			selfJson.put("Host", mHost);
			selfJson.put("Port", mPort);
			if (mStorePassword) {
				selfJson.put("Password", mPassword);
			}
			selfJson.put("RemotePath", mRemotePath);
			selfJson.put("LocalPath", mLocalPath);
			selfJson.put("Options", mOptions);
			selfJson.put("RootDir", mRootDir);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}

		return selfJson;
	}

	public void json(JSONObject selfJson) {
		mPointName = selfJson.optString("PointName", mPointName);
		mAutoMount = selfJson.optBoolean("AutoMount", mAutoMount);
		mUserName = selfJson.optString("UserName", mUserName);
		mHost = selfJson.optString("Host", mHost);
		mPort = selfJson.optInt("Port", mPort);
		mStorePassword = selfJson.optBoolean("StorePassword", mStorePassword);
		if (selfJson.has("Password")) {
			mStorePassword = true;
			mPassword = selfJson.optString("Password", mPassword);
		}
		else {
			mStorePassword = false;
			mPassword = "";
		}
		mRemotePath = selfJson.optString("RemotePath", mRemotePath);
		mLocalPath = selfJson.optString("LocalPath", mLocalPath);
		mOptions = selfJson.optString("Options", mOptions);
		mRootDir = selfJson.optString("RootDir", mRootDir);
	}

	private String getHostIp() {
		try {
			InetAddress address = InetAddress.getByName(getHost());
			if (address instanceof Inet6Address) {
				if (!address.getHostAddress().startsWith("[")) {
					return '[' + address.getHostAddress() + ']';
				}
			}
			return address.getHostAddress();

		} catch (UnknownHostException e) {
			logMessage(e.getMessage());
			return getHost();
		}
	}

	private void logMessage(final String message) {
		LogSingleton.getLogModel().addMessage(message);
	}

	public boolean isMounted() {
		return mIsMounted;
	}

	public void checkMount(final Context context) {
		checkMount(false, context);
	}

	public void checkMount(final boolean verbose, final Context context) {
		new CheckMountTask(verbose, context).execute();
	}

	public void mount(final Context context) {
		mount(false, context);
	}

	public void mount(boolean verbose, final Context context) {
		logMessage("mount");
		new MountTask(verbose, context).execute();
	}

	public void umount(final Context context) {
		umount(false, context);
	}

	public void umount(final boolean verbose, final Context context) {
		String umountCommand = "umount ";
		if (RootShell.isBusyboxAvailable()) {
			umountCommand = "busybox umount -f ";
		}
		logMessage(umountCommand);
		runCommand(umountCommand + getLocalPath(), verbose, context);
	}

	public void registerObserver(final Observer observer, final Context context) {
		mObservable.registerObserver(observer);
		checkMount(context);
	}

	public void unregisterObserver(final Observer observer) {
		mObservable.unregisterObserver(observer);
	}

	public interface Observer {
		void onMountStateChanged(final MountPoint mountPoint);
	}

	private class MountObservable extends Observable<Observer> {
		public void notifyChanged() {
			for (final Observer observer : mObservers) {
				observer.onMountStateChanged(MountPoint.this);
			}
		}
	}

	private class CheckMountTask extends AsyncTask<Void, Void, Pair<Boolean, String>> {
		private final String mMountFile = "/proc/mounts";
		private final boolean mVerbose;
		private final Context mContext;

		private CheckMountTask(final boolean verbose, final Context context) {
			this.mVerbose = verbose;
			this.mContext = context;
		}

		@Override
		protected Pair<Boolean, String> doInBackground(Void... params) {
			final StringBuilder mountLine = new StringBuilder();
			mountLine.append(getUserName()).append('@');
			mountLine.append(getHostIp()).append(':');

			String canonicalLocalPath = getLocalPath();
			try {
				canonicalLocalPath = new File(canonicalLocalPath).getCanonicalPath();
			} catch (IOException e) {
				logMessage("Can't get canonical path of " + getLocalPath() + " : " + e.getMessage());
			}

			mountLine.append(getRemotePath()).append(' ').append(canonicalLocalPath);
			mountLine.append(' ').append("fuse.sshfs").append(' ');

			boolean result = false;

			try {
				FileInputStream fstream = new FileInputStream(mMountFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

				String line;
				while (!result && (line = br.readLine()) != null) {
					result = line.contains(mountLine.toString());
				}

				br.close();
			} catch (FileNotFoundException e) {
				return new Pair<>(result, e.getMessage());
			} catch (IOException e) {
				return new Pair<>(result, e.getMessage());
			}

			mIsMounted = result;
			if (result) {
				return new Pair<>(result, "Pattern " + mountLine.toString() + " is in " + mMountFile);
			}
			else {
				return new Pair<>(result, "Pattern " + mountLine.toString() + " is not in " + mMountFile);
			}
		}

		@Override
		protected void onPostExecute(final Pair<Boolean, String> result) {
			mIsMounted = result.first;
			logMessage(result.second);

			mObservable.notifyChanged();
			if (mVerbose) {
				EasySSHFSActivity.showToast("done", mContext);
			}
		}
	}

	private class MountTask extends AsyncTask<Void, Void, String> {
		private final boolean mVerbose;
		private final Context mContext;

		private MountTask(boolean verbose, final Context context) {
			this.mVerbose = verbose;
			this.mContext = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			return getHostIp();
		}

		@Override
		protected void onPostExecute(final String hostIp) {
			final StringBuilder command = new StringBuilder();
			command.append("echo '").append(getPassword()).append("' | ");
			command.append(mRootDir).append("/sshfs");
			command.append(" -o 'ssh_command=").append(mRootDir).append("/ssh").append(',');
			command.append(getOptions()).append(",port=").append(getPort()).append("' ");
			command.append(getUserName()).append('@').append(hostIp).append(':');
			command.append(getRemotePath()).append(' ').append(getLocalPath());

			runCommand(command.toString(), mVerbose, mContext);
		}
	}

	private void runCommand(final String command, final boolean verbose, final Context context) {
		try {
			Shell shell = RootShell.getShell(true);
			Command cmd = new Command(commandCode++, command) {
				@Override
				public void commandOutput(int id, String line)
				{
					logMessage(line);
				}

				@Override
				public void commandTerminated(int id, String reason)
				{
					logMessage("Terminated: " + reason);
					checkMount(verbose, context);
				}
				@Override
				public void commandCompleted(int id, int exitCode)
				{
					logMessage("Completed with code " + exitCode);
					checkMount(verbose, context);
				}
			};
			shell.add(cmd);
		} catch (RootDeniedException | TimeoutException | IOException e) {
			logMessage(e.getMessage());
		}
	}

}
