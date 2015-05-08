package ru.nsu.bobrofon.easysshfs.log;

import android.database.Observable;
import android.util.Log;

public class LogModel {
	private static final String TAG = "LogModel";

	private final LogObservable mObservable = new LogObservable();
	private StringBuffer mLogBuffer = new StringBuffer();

	public LogModel() {
		Log.i(TAG, "new Instance");
	}

	public String getLog() {
		return mLogBuffer.toString();
	}

	public void addMessage(CharSequence message) {
		Log.i(TAG, "new message: " + message);
		mLogBuffer.append(message).append("\n");
		mObservable.notifyChanged();
	}

	public void clean() {
		mLogBuffer = new StringBuffer();
		mObservable.notifyChanged();
	}

	public void registerObserver(final Observer observer) {
		mObservable.registerObserver(observer);
		observer.onLogChanged(this);
	}

	public void unregisterObserver(final Observer observer) {
		mObservable.unregisterObserver(observer);
	}

	public interface Observer {
		void onLogChanged(LogModel logModel);
	}

	private class LogObservable extends Observable<Observer> {
		public void notifyChanged() {
			for (final Observer observer : mObservers) {
				observer.onLogChanged(LogModel.this);
			}
		}
	}
}
