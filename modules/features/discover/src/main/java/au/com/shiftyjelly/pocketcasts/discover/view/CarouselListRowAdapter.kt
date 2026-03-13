package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.automattic.eventhorizon.DiscoverFeaturedPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val differ: DiffUtil.ItemCallback<Any> = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem::class.java == newItem::class.java) {
            if (oldItem is DiscoverPodcast && newItem is DiscoverPodcast) {
                return oldItem.uuid == newItem.uuid
            }
        }

        return false
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is DiscoverPodcast && newItem is DiscoverPodcast) {
            return oldItem.hashCode() == newItem.hashCode()
        }

        return true
    }
}

internal class CarouselListRowAdapter(
    var pillText: String?,
    val theme: Theme,
    val onPodcastClicked: ((DiscoverPodcast, String?, String?, Boolean) -> Unit),
    val onPodcastSubscribe: ((DiscoverPodcast, String?) -> Unit),
    private val eventHorizon: EventHorizon,
) : ListAdapter<Any, CarouselItemViewHolder>(differ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
        return CarouselItemViewHolder(theme, view)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val podcast = getItem(position)
        val context = holder.itemView.context
        if (podcast is DiscoverPodcast) {
            val tagLineText = if (podcast.isSponsored) {
                context.getString(LR.string.discover_sponsored)
            } else {
                pillText
            }
            holder.setPodcast(podcast = podcast)

            holder.setTaglineText(tagLineText)
            holder.itemView.setOnClickListener {
                val isFeatured = podcast.isSponsored || podcast.listId == null
                onPodcastClicked(podcast, podcast.listId, null, isFeatured)

                podcast.listId?.let { listId ->
                    eventHorizon.track(
                        DiscoverListPodcastTappedEvent(
                            listId = listId,
                            podcastUuid = podcast.uuid,
                        ),
                    )
                }
                if (isFeatured) {
                    eventHorizon.track(
                        DiscoverFeaturedPodcastTappedEvent(
                            podcastUuid = podcast.uuid,
                        ),
                    )
                }
            }
            holder.btnSubscribe.setOnClickListener {
                val isFeatured = podcast.isSponsored || podcast.listId == null
                holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = true)
                onPodcastSubscribe(podcast, null)

                podcast.listId?.let { listId ->
                    eventHorizon.track(
                        DiscoverListPodcastSubscribedEvent(
                            listId = listId,
                            podcastUuid = podcast.uuid,
                        ),
                    )
                }
                if (isFeatured) {
                    eventHorizon.track(
                        DiscoverFeaturedPodcastSubscribedEvent(
                            podcastUuid = podcast.uuid,
                        ),
                    )
                }
            }
        } else {
            holder.setPodcast(podcast = null)
        }
    }
}
