package ru.nsu.bobrofon.easysshfs.log;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.nsu.bobrofon.easysshfs.DrawerStatus;
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity;
import ru.nsu.bobrofon.easysshfs.R;

public class LogFragment extends Fragment implements LogModel.Observer {
	private static final String TAG = "LogFragment";

	private TextView mLogTextView;
	private LogModel mLogModel;
	private DrawerStatus mDrawerStatus;

	public LogFragment() {
		// Required empty public constructor
	}

	public void setDrawerStatus(final DrawerStatus drawerStatus) {
		mDrawerStatus = drawerStatus;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);

		// Inflate the layout for this fragment
		View selfView = inflater.inflate(R.layout.fragment_log, container, false);

		mLogTextView = selfView.findViewById(R.id.log);

		mLogModel = LogSingleton.getLogModel();

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
		mLogTextView.setText(String.format("%s%s", logHeader, logBody));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((EasySSHFSActivity) activity).onSectionAttached(R.string.debug_log_title);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (mDrawerStatus == null || !mDrawerStatus.isDrawerOpen()) {
			inflater.inflate(R.menu.log, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_clean) {
			mLogModel.clean();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
