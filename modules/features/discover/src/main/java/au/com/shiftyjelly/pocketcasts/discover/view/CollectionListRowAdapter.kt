package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionListViewHolder.Companion.NUMBER_OF_ROWS_PER_PAGE
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val differ = object : DiffUtil.ItemCallback<List<Any>>() {
    override fun areItemsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
        return oldItem.filterIsInstance<DiscoverPodcast>()
            .map { it.uuid }
            .toTypedArray()
            .contentDeepEquals(
                newItem
                    .filterIsInstance<DiscoverPodcast>()
                    .map { it.uuid }.toTypedArray(),
            )
    }

    override fun areContentsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
        return oldItem.filterIsInstance<DiscoverPodcast>().toTypedArray().contentDeepEquals(newItem.filterIsInstance<DiscoverPodcast>().toTypedArray())
    }
}

internal class CollectionListRowAdapter(
    val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit),
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    val onHeaderClicked: () -> Unit,
    val analyticsTracker: AnalyticsTracker,
) : ListAdapter<List<Any>, CollectionListRowAdapter.CollectionListViewHolder>(differ) {

    class CollectionListViewHolder(
        val binding: ItemCollectionListBinding,
        onItemClicked: (Int, Int) -> Unit,
        onHeaderClicked: () -> Unit,
        onPodcastSubscribe: (Int, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val imageRequestFactory = PocketCastsImageRequestFactory(
            binding.root.context,
            placeholderType = PlaceholderType.Large,
        ).themed()

        companion object {
            const val NUMBER_OF_ROWS_PER_PAGE = 2
            const val NUMBER_OF_PODCASTS_TO_DISPLAY_TWICE = 2
        }

        init {
            binding.row0.setOnClickListener {
                onItemClicked(bindingAdapterPosition, 0)
            }
            binding.row1.setOnClickListener {
                onItemClicked(bindingAdapterPosition, 1)
            }
            binding.header.root.setOnClickListener {
                onHeaderClicked()
            }
            binding.row0.onSubscribeClicked = {
                onPodcastSubscribe(bindingAdapterPosition, 0)
            }
            binding.row1.onSubscribeClicked = {
                onPodcastSubscribe(bindingAdapterPosition, 1)
            }
        }

        val rows = listOf(binding.row0, binding.row1)

        fun bindPodcasts(podcastSublist: List<Any>, isSubscribeButtonVisible: Boolean) {
            rows.forEachIndexed { index, row ->
                row.clear()
                row.isVisible = index < podcastSublist.size

                val podcast = podcastSublist.getOrNull(index) as? DiscoverPodcast
                row.podcast = podcast

                val isLandscape = row.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                row.setSubscribeButtonVisibility(isSubscribeButtonVisible || isLandscape)
            }
        }

        fun bindHeader(header: CollectionHeader) {
            binding.header.root.isVisible = true
            binding.header.lblTitle.text = header.title
            binding.header.lblSubtitle.text = header.subtitle
            imageRequestFactory.createForFileOrUrl(header.imageUrl).loadInto(binding.header.imageHeader)
            binding.header.root.contentDescription =
                binding.root.context.getString(LR.string.discover_collection_header_content_description, header.title)

            val height = getPodcastsHeight(binding.podcasts)
            val layoutParams = binding.root.layoutParams
            layoutParams.height = height
            binding.root.layoutParams = layoutParams
        }

        private fun getPodcastsHeight(view: View): Int {
            // It's necessary because it performs the calculation of the view's width and height based on its internal properties.
            // Without calling it, the measuredHeight and measuredWidth properties wouldn't be correctly defined,
            // as these properties are calculated after the view is measured.
            view.measure(0, 0)
            return view.measuredHeight
        }
    }

    private var fromListId: String? = null
    private var header: CollectionHeader? = null

    fun submitPodcastList(list: List<DiscoverPodcast>, header: CollectionHeader?, commitCallback: Runnable?) {
        this.header = header
        val chunkedList = list.chunked(NUMBER_OF_ROWS_PER_PAGE)

        // We want to display the first two podcasts on the next page as well, without the header. That's why we duplicate them in the list.
        val modifiedList = if (chunkedList.isNotEmpty()) {
            listOf(chunkedList[0]) + chunkedList
        } else {
            chunkedList
        }
        submitList(modifiedList, commitCallback)
    }

    fun showLoadingList() {
        val loadingList = listOf(MutableList(NUMBER_OF_ROWS_PER_PAGE) { LoadingItem() })
        submitList(loadingList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCollectionListBinding.inflate(inflater, parent, false)

        return CollectionListViewHolder(
            binding,
            onItemClicked = { pageIndex, podcastIndex ->
                val podcastSublist = getItem(pageIndex)
                val podcast = podcastSublist.getOrNull(podcastIndex) as? DiscoverPodcast

                if (podcast == null) return@CollectionListViewHolder

                fromListId?.let {
                    analyticsTracker.track(
                        AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
                        mapOf(
                            LIST_ID_KEY to it,
                            PODCAST_UUID_KEY to podcast.uuid,
                        ),
                    )
                }
                onPodcastClicked(podcast, fromListId)
            },
            onHeaderClicked = {
                onHeaderClicked()
            },
            onPodcastSubscribe = { pageIndex, podcastIndex ->
                val podcastSublist = getItem(pageIndex)
                val podcast = podcastSublist.getOrNull(podcastIndex) as? DiscoverPodcast
                podcast?.let { onPodcastSubscribe(it, fromListId) }
            },
        )
    }

    override fun onBindViewHolder(holder: CollectionListViewHolder, position: Int) {
        val podcastSublist = getItem(position)

        holder.bindPodcasts(podcastSublist, isSubscribeButtonVisible = position != 0)

        if (position == 0) {
            this.header?.let { holder.bindHeader(it) }
        } else {
            holder.binding.header.root.isVisible = false
        }
    }

    fun setFromListId(value: String) {
        this.fromListId = value
    }

    data class CollectionHeader(val imageUrl: String, val title: String?, val subtitle: String?)
}
