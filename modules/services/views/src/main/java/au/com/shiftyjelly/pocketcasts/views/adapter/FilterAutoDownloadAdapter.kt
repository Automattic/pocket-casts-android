package au.com.shiftyjelly.pocketcasts.views.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.helper.PlaylistHelper

class FilterAutoDownloadAdapter(private val filters: List<Playlist>, private val clickListener: ClickListener, val isDarkTheme: Boolean) : androidx.recyclerview.widget.RecyclerView.Adapter<FilterAutoDownloadAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.adapter_filter_auto_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filter = filters[position]

        holder.title.text = filter.title
        holder.checkBox.isChecked = filter.autoDownload
        PlaylistHelper.updateImageView(filter, holder.image)

        addTalkBack(filter, holder.button)
    }

    override fun getItemCount(): Int {
        return filters.size
    }

    override fun getItemId(position: Int): Long {
        return filters[position].id ?: androidx.recyclerview.widget.RecyclerView.NO_ID
    }

    private fun addTalkBack(filter: Playlist, view: View) {
        val status = if (filter.autoDownload) "on" else "off"
        view.contentDescription = "${filter.title} auto downloads $status."
    }

    interface ClickListener {
        fun onAutoDownloadChanged(filter: Playlist, on: Boolean)
        fun onSettingsClicked(filter: Playlist)
    }

    inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view), View.OnClickListener {
        val title = view.findViewById(R.id.title) as TextView
        val image = view.findViewById(R.id.image) as ImageView
        val button = view.findViewById(R.id.row_button) as View
        val checkBox = view.findViewById(R.id.checkbox) as CheckBox
        val settingsButton = view.findViewById(R.id.settings_button) as ImageView

        init {
            button.setOnClickListener(this)
            checkBox.setOnClickListener(this)
            settingsButton.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val filter = filters[bindingAdapterPosition]
            if (view.id == R.id.row_button) {
                checkBox.isChecked = !checkBox.isChecked
            }
            if (view.id == R.id.checkbox || view.id == R.id.row_button) {
                clickListener.onAutoDownloadChanged(filter, checkBox.isChecked)
            } else if (view.id == R.id.settings_button) {
                clickListener.onSettingsClicked(filter)
            }
        }
    }
}
