package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.discover.util.DISCOVER_PODCAST_DIFF_CALLBACK
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal class LargeListRowAdapter(val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit), val onPodcastSubscribe: ((DiscoverPodcast, String?) -> Unit)) : ListAdapter<Any, LargeListRowAdapter.LargeListItemViewHolder>(DISCOVER_PODCAST_DIFF_CALLBACK) {
    var fromListId: String? = null

    class LargeListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeholderDrawable = itemView.context.getThemeDrawable(UR.attr.defaultArtworkSmall)
        val imageView = itemView.findViewById<ImageView>(R.id.imageView)
        val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)
        val lblSubtitle = itemView.findViewById<TextView>(R.id.lblSubtitle)
        val btnSubscribe = itemView.findViewById<ImageButton>(R.id.btnSubscribe)
    }

    companion object {
        const val NUMBER_OF_LOADING_ITEMS = 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LargeListItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_large_list, parent, false)
        return LargeListItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LargeListItemViewHolder, position: Int) {
        val podcast = getItem(position)
        val context = holder.imageView.context
        val imageLoader = PodcastImageLoaderThemed(context).smallPlaceholder()

        if (podcast is DiscoverPodcast) {
            imageLoader.loadPodcastUuid(podcast.uuid).into(holder.imageView)

            holder.lblTitle.text = podcast.title
            holder.lblSubtitle.text = podcast.author
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener {
                fromListId?.let { FirebaseAnalyticsTracker.podcastTappedFromList(it, podcast.uuid) }
                onPodcastClicked(podcast, fromListId)
            }
            holder.btnSubscribe.isClickable = true
            holder.btnSubscribe.setOnClickListener {
                holder.btnSubscribe.updateSubscribeButtonIcon(subscribed = true, colorSubscribed = UR.attr.contrast_01, colorUnsubscribed = UR.attr.contrast_01)
                fromListId?.let { FirebaseAnalyticsTracker.podcastSubscribedFromList(it, podcast.uuid) }
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
}
