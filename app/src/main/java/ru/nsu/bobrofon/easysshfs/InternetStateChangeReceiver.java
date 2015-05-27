package ru.nsu.bobrofon.easysshfs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import ru.nsu.bobrofon.easysshfs.mountpointList.MountPointsList;

public class InternetStateChangeReceiver extends BroadcastReceiver {
	public InternetStateChangeReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if(info != null) {
			if(info.isConnected()) {
				MountPointsList.getIntent(context).autoMount();
			}
		}
	}
}
