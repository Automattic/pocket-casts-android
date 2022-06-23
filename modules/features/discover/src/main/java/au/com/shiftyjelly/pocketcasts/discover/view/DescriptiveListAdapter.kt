package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

private val differ = object : DiffUtil.ItemCallback<DiscoverPodcast>() {
    override fun areItemsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem == newItem
    }
}

class DescriptiveListAdapter(val onPodcastClick: (DiscoverPodcast) -> Unit, val onPodcastSubscribe: (String) -> Unit) : ListAdapter<DiscoverPodcast, DescriptiveListAdapter.PodcastViewHolder>(differ) {

    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val podcastDescriptiveRow: PodcastDescriptiveRow
            get() = this.itemView as PodcastDescriptiveRow
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val podcastDescriptiveRow = PodcastDescriptiveRow(parent.context)
        return PodcastViewHolder(podcastDescriptiveRow)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.podcastDescriptiveRow.podcast = podcast
        holder.itemView.setOnClickListener {
            onPodcastClick(podcast)
        }
        holder.podcastDescriptiveRow.onSubscribeClicked = {
            onPodcastSubscribe(podcast.uuid)
        }
    }
}
