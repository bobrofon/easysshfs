package ru.nsu.bobrofon.easysshfs.log;

public class LogSingleton {
	private final static LogModel mLogModel = new LogModel();

	public static LogModel getLogModel() {
		return mLogModel;
	}

}
