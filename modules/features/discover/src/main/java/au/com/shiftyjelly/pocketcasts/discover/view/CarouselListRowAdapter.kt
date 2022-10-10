package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

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

internal class CarouselListRowAdapter(var pillText: String?, val theme: Theme, val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit), val onPodcastSubscribe: ((DiscoverPodcast, String?) -> Unit), private val analyticsTracker: AnalyticsTrackerWrapper) : ListAdapter<Any, CarouselItemViewHolder>(differ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
        return CarouselItemViewHolder(theme, view)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val podcast = getItem(position)
        if (podcast is DiscoverPodcast) {
            holder.podcast = podcast
            holder.setTaglineText(pillText)
            val properties = mapOf(PODCAST_UUID_KEY to AnalyticsPropValue(podcast.uuid))
            holder.itemView.setOnClickListener {
                onPodcastClicked(podcast, null) // no analytics for carousel

                FirebaseAnalyticsTracker.openedFeaturedPodcast()
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_TAPPED, properties)
            }
            holder.btnSubscribe.setOnClickListener {
                holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = true)
                onPodcastSubscribe(podcast, null) // no analytics for carousel

                FirebaseAnalyticsTracker.subscribedToFeaturedPodcast()
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_SUBSCRIBED, properties)
            }
        } else {
            holder.podcast = null
        }
    }

    companion object {
        private const val PODCAST_UUID_KEY = "podcast_uuid"
    }
}
