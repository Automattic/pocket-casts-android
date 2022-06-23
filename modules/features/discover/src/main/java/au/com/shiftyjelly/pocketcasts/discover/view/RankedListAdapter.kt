package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.util.DISCOVER_PODCAST_DIFF_CALLBACK
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class RankedListAdapter(
    val onPodcastClick: (DiscoverPodcast) -> Unit,
    val onPodcastSubscribe: (String) -> Unit,
    val taglineText: String?,
    val theme: Theme
) : ListAdapter<Any, RecyclerView.ViewHolder>(DISCOVER_PODCAST_DIFF_CALLBACK) {
    class RankedPodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val podcastRow: PodcastRow = itemView.findViewById(R.id.podcastRow)
        val lblRank: TextView = itemView.findViewById(R.id.lblRank)
    }

    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_top_ranked -> CarouselItemViewHolder(theme, view)
            R.layout.ranked_podcast_row -> RankedPodcastViewHolder(view)
            else -> ErrorViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val podcast = getItem(position)
        if (podcast !is DiscoverPodcast) {
            return
        }
        when (holder) {
            is CarouselItemViewHolder -> {
                holder.podcast = podcast
                holder.setRanking("1")
                holder.itemView.setOnClickListener {
                    onPodcastClick(podcast)
                }
                holder.setTaglineText(taglineText)
                holder.btnSubscribe.setOnClickListener {
                    onPodcastSubscribe(podcast.uuid)
                }
            }
            is RankedPodcastViewHolder -> {
                holder.podcastRow.podcast = podcast
                holder.itemView.setOnClickListener {
                    onPodcastClick(podcast)
                }
                holder.podcastRow.onSubscribeClicked = {
                    onPodcastSubscribe(podcast.uuid)
                }
                holder.lblRank.text = (position + 1).toString()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> R.layout.item_top_ranked
            else -> R.layout.ranked_podcast_row
        }
    }
}
