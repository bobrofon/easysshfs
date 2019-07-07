package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

import com.topjohnwu.superuser.Shell

import kotlinx.android.synthetic.main.row_layout.view.mpNameView as nameView
import kotlinx.android.synthetic.main.row_layout.view.mpStatusView as statusView
import kotlinx.android.synthetic.main.row_layout.view.mountButton

import ru.nsu.bobrofon.easysshfs.R

class MountPointsArrayAdapter(
    context: Context,
    private val values: List<MountPoint>,
    private val shell: Shell
) : ArrayAdapter<MountPoint>(context, R.layout.row_layout, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView ?: inflater.inflate(R.layout.row_layout, parent, false)

        val self = values[position]

        rowView.nameView.text = self.visiblePointName
        if (self.isMounted) {
            rowView.statusView.text = context.getString(R.string.mounted)
            rowView.mountButton.text = context.getString(R.string.umount)
            rowView.mountButton.setOnClickListener { self.umount(shell, context) }
        } else {
            rowView.statusView.text = context.getString(R.string.not_mounted)
            rowView.mountButton.text = context.getString(R.string.mount)
            rowView.mountButton.setOnClickListener { self.mount(shell, context) }
        }

        return rowView
    }

}
