package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

private val diff = object : DiffUtil.ItemCallback<DiscoverPodcast>() {
    override fun areItemsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: DiscoverPodcast, newItem: DiscoverPodcast): Boolean {
        return oldItem == newItem
    }
}

class RemainingPodcastsAdapter(
    val onPodcastClick: (DiscoverPodcast, String?) -> Unit,
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
) : ListAdapter<DiscoverPodcast, RemainingPodcastsAdapter.PodcastViewHolder>(diff) {

    inner class PodcastViewHolder(
        itemView: View,
        private val onPodcastClick: (Int) -> Unit,
        private val onPodcastSubscribe: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                onPodcastClick(bindingAdapterPosition)
            }

            (itemView as PodcastRow).onSubscribeClicked = {
                onPodcastSubscribe(bindingAdapterPosition)
            }
        }

        fun bind(podcast: DiscoverPodcast) {
            (itemView as PodcastRow).podcast = podcast
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder =
        PodcastViewHolder(
            PodcastRow(parent.context),
            onPodcastClick = {
                val podcast = getItem(it) as DiscoverPodcast
                onPodcastClick(podcast, podcast.listId)
            },
            onPodcastSubscribe = {
                val podcast = getItem(it) as DiscoverPodcast
                onPodcastSubscribe(podcast, podcast.listId)
            },
        )

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
