package ru.nsu.bobrofon.easysshfs.log;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class LogWorkerFragment extends Fragment {
	private final LogModel mLogModel;

	public LogWorkerFragment() {
		mLogModel = new LogModel();
	}


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public LogModel getLogModel() {
		return mLogModel;
	}

	public static LogModel getLogModelByTag(final FragmentManager fragmentManager,
	                                                 final String tag) {
		final LogWorkerFragment retainedWorkerFragment =
			(LogWorkerFragment) fragmentManager.findFragmentByTag(tag);

		final LogModel logModel;
		if (retainedWorkerFragment != null) {
			logModel = retainedWorkerFragment.getLogModel();
		} else {
			final LogWorkerFragment workerFragment = new LogWorkerFragment();

			fragmentManager.beginTransaction()
				.add(workerFragment, tag)
				.commit();

			logModel = workerFragment.getLogModel();
		}

		return logModel;
	}

}
