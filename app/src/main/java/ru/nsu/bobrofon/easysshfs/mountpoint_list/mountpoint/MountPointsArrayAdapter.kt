package ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.R

class MountPointsArrayAdapter(private val mContext: Context, private val mValues: List<MountPoint>,
                              private val mShell: Shell) : ArrayAdapter<MountPoint>(mContext, R.layout.row_layout, mValues) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.row_layout, parent, false)

        val nameView = rowView.findViewById<TextView>(R.id.mpNameView)
        val statusView = rowView.findViewById<TextView>(R.id.mpStatusView)
        val mountButton = rowView.findViewById<Button>(R.id.mountButton)

        val self = mValues[position]

        nameView.text = self.pointName
        if (self.isMounted) {
            statusView.text = mContext.getString(R.string.mounted)
            mountButton.text = mContext.getString(R.string.umount)
            mountButton.setOnClickListener { self.umount(true, mContext, mShell) }
        } else {
            statusView.text = mContext.getString(R.string.not_mounted)
            mountButton.text = mContext.getString(R.string.mount)
            mountButton.setOnClickListener { self.mount(true, mContext, mShell) }
        }

        return rowView
    }

}
