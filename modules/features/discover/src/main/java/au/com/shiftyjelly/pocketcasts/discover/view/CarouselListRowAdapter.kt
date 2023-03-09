package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
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

internal class CarouselListRowAdapter(var pillText: String?, val theme: Theme, val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit), val onPodcastSubscribe: ((DiscoverPodcast, String?) -> Unit), private val analyticsTracker: AnalyticsTrackerWrapper) : ListAdapter<Any, CarouselItemViewHolder>(differ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
        return CarouselItemViewHolder(theme, view)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val podcast = getItem(position)
        if (podcast is DiscoverPodcast) {
            val context = holder.itemView.context
            val tagLineText = if (podcast.isSponsored) {
                context.getString(LR.string.discover_sponsored)
            } else {
                pillText
            }
            holder.podcast = podcast

            holder.setTaglineText(tagLineText)
            holder.itemView.setOnClickListener {
                onPodcastClicked(podcast, null) // no analytics for carousel

                FirebaseAnalyticsTracker.openedFeaturedPodcast()
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_TAPPED, AnalyticsProp.featuredPodcastTapped(podcast.uuid))
            }
            holder.btnSubscribe.setOnClickListener {
                holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = true)
                onPodcastSubscribe(podcast, null) // no analytics for carousel

                FirebaseAnalyticsTracker.subscribedToFeaturedPodcast()
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_SUBSCRIBED, AnalyticsProp.featuredPodcastSubscribed(podcast.uuid))
                analyticsTracker.track(AnalyticsEvent.PODCAST_SUBSCRIBED, AnalyticsProp.podcastSubscribed(AnalyticsSource.DISCOVER, podcast.uuid))
            }
        } else {
            holder.podcast = null
        }
    }

    companion object {
        private object AnalyticsProp {
            private const val PODCAST_UUID_KEY = "podcast_uuid"
            private const val SOURCE_KEY = "source"
            private const val UUID_KEY = "uuid"
            fun featuredPodcastTapped(uuid: String) = mapOf(PODCAST_UUID_KEY to uuid)
            fun featuredPodcastSubscribed(uuid: String) = mapOf(PODCAST_UUID_KEY to uuid)
            fun podcastSubscribed(source: AnalyticsSource, uuid: String) =
                mapOf(SOURCE_KEY to source.analyticsValue, UUID_KEY to uuid)
        }
    }
}
