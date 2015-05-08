package ru.nsu.bobrofon.easysshfs.log;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.R;

public class LogFragment extends Fragment implements LogModel.Observer {
	private static final String TAG = "LogFragment";
	public static final String TAG_WORKER = "TAG_LOG_WORKER";

	private TextView mLogTextView;
	private LogModel mLogModel;

	public LogFragment() {
		// Required empty public constructor
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View selfView = inflater.inflate(R.layout.fragment_log, container, false);

		mLogTextView = (TextView) selfView.findViewById(R.id.log);

		mLogModel = LogWorkerFragment.getLogModelByTag(getFragmentManager(), TAG_WORKER);

		mLogModel.registerObserver(this);

		return selfView;
	}

	@Override
	public void onDestroyView () {
		Log.i(TAG, "onDestroyView");
		super.onDestroyView();
		mLogModel.unregisterObserver(this);
	}


	@Override
	public void onLogChanged(LogModel logModel) {
		final String logHeader = getString(R.string.debug_log_header);
		final String logBody = logModel.getLog();
		mLogTextView.setText(logHeader + logBody);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((EasySSHFSActivity) activity).onSectionAttached(R.string.debug_log_title);
	}
}
