package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemMostPopularPodcastsBinding
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.MostPopularPodcastsAdapter.MostPopularPodcastsViewHolder
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed

internal class MostPopularPodcastsAdapter(
    val onPodcastClicked: (DiscoverPodcast, String?) -> Unit,
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    private val analyticsTracker: AnalyticsTracker,
) : ListAdapter<DiscoverPodcast, MostPopularPodcastsViewHolder>(PODCASTS_FILTERED_DIFF) {

    private var fromListId: String? = null

    class MostPopularPodcastsViewHolder(
        val binding: ItemMostPopularPodcastsBinding,
        private val onItemClicked: (Int) -> Unit,
        private val onSubscribeButtonClicked: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val imageRequestFactory = PocketCastsImageRequestFactory(
            binding.root.context,
            placeholderType = PlaceholderType.Small,
        ).themed()

        init {
            binding.imageView.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
            }

            binding.btnSubscribe.setOnClickListener {
                onSubscribeButtonClicked(bindingAdapterPosition)
            }
        }

        fun bind(podcast: DiscoverPodcast) {
            imageRequestFactory.createForPodcast(podcast.uuid).loadInto(binding.imageView)

            binding.lblTitle.text = podcast.title
            binding.lblTitle.contentDescription = podcast.title

            binding.lblSubtitle.text = podcast.author
            binding.lblSubtitle.contentDescription = podcast.author

            binding.btnSubscribe.updateSubscribeButtonIcon(subscribed = podcast.isSubscribed, colorSubscribed = R.attr.contrast_01, colorUnsubscribed = R.attr.contrast_01)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MostPopularPodcastsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMostPopularPodcastsBinding.inflate(inflater, parent, false)
        return MostPopularPodcastsViewHolder(
            binding,
            onItemClicked = { position ->
                val podcast = getItem(position) as DiscoverPodcast
                trackImpression(podcast)
                onPodcastClicked(podcast, fromListId)
            },
            onSubscribeButtonClicked = { position ->
                val podcast = getItem(position) as DiscoverPodcast
                binding.btnSubscribe.updateSubscribeButtonIcon(subscribed = true, colorSubscribed = R.attr.contrast_01, colorUnsubscribed = R.attr.contrast_01)
                fromListId?.let {
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid))
                }
                onPodcastSubscribe(podcast, fromListId)
            },
        )
    }

    override fun onBindViewHolder(holder: MostPopularPodcastsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun replaceList(list: List<DiscoverPodcast>) {
        val changedPodcastList = currentList.map { it.uuid } != list.map { it.uuid }

        if (changedPodcastList) {
            submitList(null) // We need this to avoid displaying the previous category list when switching from a different category
        }

        submitList(list)
    }

    fun setFromListId(value: String) {
        this.fromListId = value
    }

    private fun trackImpression(podcast: DiscoverPodcast) {
        fromListId?.let {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
                mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcast.uuid),
            )
        }
    }
}
