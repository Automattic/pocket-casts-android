package au.com.shiftyjelly.pocketcasts.discover.util

import androidx.recyclerview.widget.DiffUtil
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

val DISCOVER_PODCAST_DIFF_CALLBACK = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem is DiscoverPodcast && newItem is DiscoverPodcast && oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}
