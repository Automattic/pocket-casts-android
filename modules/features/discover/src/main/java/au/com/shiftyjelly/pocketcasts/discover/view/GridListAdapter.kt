package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx

private val differ = object : DiffUtil.ItemCallback<DiscoverPodcast>() {
    override fun areItemsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem == newItem
    }
}

class GridListAdapter(
    private val onPodcastClicked: ((DiscoverPodcast) -> Unit),
    private val onPodcastSubscribe: ((String) -> Unit),
    private val imageSize: Int = 200,
) : ListAdapter<DiscoverPodcast, GridListAdapter.PodcastViewHolder>(differ) {
    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val podcastGridRow: PodcastGridRow
            get() = this.itemView as PodcastGridRow
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val podcastGridRow = PodcastGridRow(context = parent.context).apply {
            updatePadding(bottom = 16.dpToPx(parent.context))
            updateImageSize(imageSize = imageSize)
        }
        return PodcastViewHolder(podcastGridRow)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.podcastGridRow.podcast = podcast
        holder.podcastGridRow.onPodcastClicked = {
            onPodcastClicked(podcast)
        }
        holder.podcastGridRow.onSubscribeClicked = {
            onPodcastSubscribe(podcast.uuid)
        }
    }
}
