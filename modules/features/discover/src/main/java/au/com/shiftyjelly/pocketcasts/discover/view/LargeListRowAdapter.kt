package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.discover.util.DISCOVER_PODCAST_DIFF_CALLBACK
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal class LargeListRowAdapter(
    context: Context,
    val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit),
    val onPodcastSubscribe: ((DiscoverPodcast, String?) -> Unit),
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ListAdapter<Any, LargeListRowAdapter.LargeListItemViewHolder>(DISCOVER_PODCAST_DIFF_CALLBACK) {
    private var fromListId: String? = null

    class LargeListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeholderDrawable = itemView.context.getThemeDrawable(UR.attr.defaultArtworkSmall)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val lblTitle: TextView = itemView.findViewById(R.id.lblTitle)
        val lblSubtitle: TextView = itemView.findViewById(R.id.lblSubtitle)
        val btnSubscribe: ImageButton = itemView.findViewById(R.id.btnSubscribe)
    }

    companion object {
        const val NUMBER_OF_LOADING_ITEMS = 3
    }

    private val imageRequestFactory = PocketCastsImageRequestFactory(context, placeholderType = PlaceholderType.Small).themed()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LargeListItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_large_list, parent, false)
        return LargeListItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LargeListItemViewHolder, position: Int) {
        val podcast = getItem(position)

        if (podcast is DiscoverPodcast) {
            imageRequestFactory.createForPodcast(podcast.uuid).loadInto(holder.imageView)

            holder.lblTitle.text = podcast.title
            holder.lblSubtitle.text = podcast.author
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener {
                fromListId?.let {
                    FirebaseAnalyticsTracker.podcastTappedFromList(it, podcast.uuid)
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid))
                }
                onPodcastClicked(podcast, fromListId)
            }
            holder.btnSubscribe.isClickable = true
            holder.btnSubscribe.setOnClickListener {
                holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = true, colorSubscribed = UR.attr.contrast_01, colorUnsubscribed = UR.attr.contrast_01)
                fromListId?.let {
                    FirebaseAnalyticsTracker.podcastSubscribedFromList(it, podcast.uuid)
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid))
                }
                onPodcastSubscribe(podcast, fromListId)
            }
            holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = podcast.isSubscribed, colorSubscribed = UR.attr.contrast_01, colorUnsubscribed = UR.attr.contrast_01)
            holder.btnSubscribe.isVisible = true
        } else {
            holder.imageView.setImageResource(holder.placeholderDrawable)
            holder.lblTitle.text = ""
            holder.lblSubtitle.text = ""
            holder.itemView.isClickable = false
            holder.btnSubscribe.isClickable = false
            holder.btnSubscribe.isVisible = false
        }
    }
    fun showLoadingList() {
        val loadingList = listOf(MutableList(NUMBER_OF_LOADING_ITEMS) { LoadingItem() })
        submitList(loadingList)
    }
    fun setFromListId(value: String) {
        this.fromListId = value
    }
}
