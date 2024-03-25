package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemMostPopularPodcastsBinding
import au.com.shiftyjelly.pocketcasts.discover.util.DISCOVER_PODCAST_DIFF_CALLBACK
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.MostPopularPodcastsAdapter.MostPopularPodcastsViewHolder
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed

internal class MostPopularPodcastsAdapter(
    val onPodcastClicked: (DiscoverPodcast, String?) -> Unit,
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ListAdapter<Any, MostPopularPodcastsViewHolder>(DISCOVER_PODCAST_DIFF_CALLBACK) {

    var fromListId: String? = null

    class MostPopularPodcastsViewHolder(
        val binding: ItemMostPopularPodcastsBinding,
        private val onItemClicked: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val imageRequestFactory = PocketCastsImageRequestFactory(
            binding.root.context,
            placeholderType = PlaceholderType.Small,
        ).themed()

        init {
            binding.podcast.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
            }
        }

        fun bind(podcast: DiscoverPodcast) {
            imageRequestFactory.createForPodcast(podcast.uuid).loadInto(binding.imageView)

            binding.lblTitle.text = podcast.title
            binding.lblTitle.contentDescription = podcast.title

            binding.lblSubtitle.text = podcast.author
            binding.lblSubtitle.contentDescription = podcast.author
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MostPopularPodcastsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMostPopularPodcastsBinding.inflate(inflater, parent, false)
        return MostPopularPodcastsViewHolder(binding) { position ->
            val podcast = getItem(position) as DiscoverPodcast
            fromListId?.let {
                FirebaseAnalyticsTracker.podcastTappedFromList(it, podcast.uuid)
                analyticsTracker.track(
                    AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
                    mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid),
                )
            }
            onPodcastClicked(podcast, fromListId)
        }
    }

    override fun onBindViewHolder(holder: MostPopularPodcastsViewHolder, position: Int) {
        val podcast = getItem(position)

        if (podcast is DiscoverPodcast) {
            holder.bind(podcast)
        }
    }
    fun replaceList(list: List<DiscoverPodcast>) {
        submitList(null) // We need this to avoid displaying the previous category list when switching from a different category
        submitList(list)
    }
}
