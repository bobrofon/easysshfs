// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.databinding.RowLayoutBinding

class MountPointsArrayAdapter(
    context: Context,
    private val values: List<MountPoint>,
    private val shell: Shell,
    private val buttonOnFocusChangeListener: View.OnFocusChangeListener,
    private val buttonOnKeyListener: View.OnKeyListener
) : ArrayAdapter<MountPoint>(context, R.layout.row_layout, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView ?: RowLayoutBinding.inflate(inflater, parent, false).root
        val nameView = rowView.findViewById<TextView>(R.id.mpNameView)
        val statusView = rowView.findViewById<TextView>(R.id.mpStatusView)
        val mountButton = rowView.findViewById<Button>(R.id.mountButton)

        val self = values[position]

        nameView.text = self.visiblePointName
        if (self.isMounted) {
            statusView.text = context.getString(R.string.mounted)
            mountButton.text = context.getString(R.string.umount)
            mountButton.setOnClickListener { self.umount(shell, context) }
        } else {
            statusView.text = context.getString(R.string.not_mounted)
            mountButton.text = context.getString(R.string.mount)
            mountButton.setOnClickListener { self.mount(shell, context) }
        }
        mountButton.onFocusChangeListener = buttonOnFocusChangeListener
        mountButton.setOnKeyListener(buttonOnKeyListener)

        return rowView
    }

}
