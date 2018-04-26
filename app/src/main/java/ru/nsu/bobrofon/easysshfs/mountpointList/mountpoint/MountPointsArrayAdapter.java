package ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import ru.nsu.bobrofon.easysshfs.R;

public class MountPointsArrayAdapter extends ArrayAdapter<MountPoint> {
	private final Context mContext;
	private final List<MountPoint> mValues;

	public MountPointsArrayAdapter(final Context context, final List<MountPoint> values) {
		super(context, R.layout.row_layout, values);
		mContext = context;
		mValues = values;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		final LayoutInflater inflater = (LayoutInflater) mContext
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		final View rowView = inflater.inflate(R.layout.row_layout, parent, false);

		final TextView nameView = rowView.findViewById(R.id.mpNameView);
		final TextView statusView = rowView.findViewById(R.id.mpStatusView);
		final Button mountButton = rowView.findViewById(R.id.mountButton);

		final MountPoint self = mValues.get(position);

		nameView.setText(self.getPointName());
		if (self.isMounted()) {
			statusView.setText(mContext.getString(R.string.mounted));
			mountButton.setText(mContext.getString(R.string.umount));
			mountButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					self.umount(true, mContext);
				}
			});
		}
		else {
			statusView.setText(mContext.getString(R.string.not_mounted));
			mountButton.setText(mContext.getString(R.string.mount));
			mountButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					self.mount(true, mContext);
				}
			});
		}

		return rowView;
	}

}
