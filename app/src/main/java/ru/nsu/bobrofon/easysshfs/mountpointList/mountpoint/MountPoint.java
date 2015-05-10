package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;

import android.database.Observable;
import android.os.AsyncTask;
import android.util.Pair;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import ru.nsu.bobrofon.easysshfs.log.LogModel;

public class MountPoint implements Serializable {
	private static transient int commandCode = 0;

	private String mPointName;
	private boolean mAutoMount;
	private String mUserName;
	private String mHost;
	private short mPort;
	private transient String mPassword;
	private String mStoredPassword;
	private boolean mStorePassword;
	private String mRemotePath;
	private String mLocalPath;
	private String mOptions;
	private String mRootDir;

	private transient LogModel mLog;
	private transient MountObservable mObservable = new MountObservable();
	private transient boolean mIsMounted = false;

	final static String DEFAULT_OPTIONS =     "password_stdin,"
											+ "UserKnownHostsFile=/dev/null,"
											+ "StrictHostKeyChecking=no,"
											+ "rw,"
											+ "dirsync,"
											+ "nosuid,"
											+ "nodev,"
											+ "noexec,"
											+ "umask=0702,"
											+ "allow_other";

	public MountPoint() {
		mPointName = "";
		mAutoMount = false;
		mUserName = "";
		mHost = "";
		mPort = 22;
		mPassword = "";
		mStoredPassword = "";
		mStorePassword = false;
		mRemotePath = "";
		mLocalPath = "";
		mOptions = DEFAULT_OPTIONS;
		mRootDir = "";
	}

	public void init(final LogModel logModel) {
		if (mPassword == null) {
			mPassword = "";
		}
		mLog = logModel;
		if (mObservable == null) {
			mObservable = new MountObservable();
		}

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

	public void setPort(final short port) {
		mPort = port;
	}

	public void setPort(final String port) {
		try {
			mPort = Short.parseShort(port);
		}
		catch (final NumberFormatException e) {
			mPort = 22;
		}
	}

	public void setPassword(final String password) {
		mPassword = password;
		if (mStorePassword) {
			mStoredPassword = mPassword;
		}
	}

	public void setStorePassword(final boolean storePassword) {
		mStorePassword = storePassword;
		if (mStorePassword) {
			mStoredPassword = mPassword;
		}
		else {
			mStoredPassword = "";
		}
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

	public void setLog(final LogModel log) {
		mLog = log;
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

	public short getPort() {
		return mPort;
	}

	public String getPassword() {
		if (mStorePassword) {
			return mStoredPassword;
		}
		else {
			return mPassword;
		}
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

	private String getHostIp() {
		try {
			return InetAddress.getByName(getHost()).getHostAddress();
		} catch (UnknownHostException e) {
			logMessage(e.getMessage());
			return getHost();
		}
	}

	private void logMessage(final String message) {
		if (mLog != null) {
			mLog.addMessage(message);
		}
	}

	public boolean isMounted() {
		return mIsMounted;
	}

	public void checkMount() {
		new CheckMountTask().execute();
	}

	public void mount() {
		new MountTask().execute();
	}

	public void umount() {
		final StringBuilder command = new StringBuilder();
		command.append("umount ").append(getLocalPath());
		runCommand(command.toString());
	}

	public void registerObserver(final Observer observer) {
		mObservable.registerObserver(observer);
		observer.onMountStateChanged(this);
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

		@Override
		protected Pair<Boolean, String> doInBackground(Void... params) {
			final StringBuilder mountLine = new StringBuilder();
			mountLine.append(getUserName()).append('@');
			mountLine.append(getHostIp()).append(':');
			mountLine.append(getRemotePath()).append(' ').append(getLocalPath());
			mountLine.append(' ').append("fuse.sshfs").append(' ');

			boolean result = false;

			try {
				FileInputStream fstream = new FileInputStream(mMountFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

				String line;
				while (result && (line = br.readLine()) != null) {
					result = line.contains(mountLine.toString());
				}

				br.close();
			} catch (FileNotFoundException e) {
				return new Pair(result, e.getMessage());
			} catch (IOException e) {
				return new Pair(result, e.getMessage());
			}

			mIsMounted = result;
			if (result) {
				return new Pair(result, "Pattern " + mountLine.toString() + " is in " + mMountFile);
			}
			else {
				return new Pair(result, "Pattern " + mountLine.toString() + " is not in " + mMountFile);
			}
		}

		@Override
		protected void onPostExecute(final Pair<Boolean, String> result) {
			mIsMounted = result.first;
			logMessage(result.second);

			mObservable.notifyChanged();
		}
	}

	private class MountTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			return getHostIp();
		}

		@Override
		protected void onPostExecute(final String hostIp) {
			final StringBuilder command = new StringBuilder();
			command.append("echo ").append(getPassword()).append(" | ");
			command.append(mRootDir).append("/sshfs");
			command.append(" -o 'ssh_command=").append(mRootDir).append("/ssh").append(',');
			command.append(getOptions()).append(",port=").append(getPort()).append("' ");
			command.append(getUserName()).append('@').append(hostIp).append(':');
			command.append(getRemotePath()).append(' ').append(getLocalPath());

			runCommand(command.toString());
		}
	}

	private void runCommand(final String command) {
		try {
			Shell shell = RootShell.getShell(true);
			Command cmd = new Command(commandCode++, 1000, command) {
				@Override
				public void commandOutput(int id, String line)
				{
					logMessage(line);
				}

				@Override
				public void commandTerminated(int id, String reason)
				{
					logMessage("Terminated: " + reason);
					checkMount();
				}
				@Override
				public void commandCompleted(int id, int exitCode)
				{
					logMessage("Completed with code " + exitCode);
					checkMount();
				}
			};
			shell.add(cmd);
		} catch (RootDeniedException e) {
			logMessage(e.getMessage());
		} catch (TimeoutException e) {
			logMessage(e.getMessage());
		} catch (IOException e) {
			logMessage(e.getMessage());
		}
	}

}
