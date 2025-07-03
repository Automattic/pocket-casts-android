package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.CollectionHeaderBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionItem
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionItem.CollectionHeader
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionItem.CollectionPodcast
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.PodcastsViewHolder.Companion.NUMBER_OF_ROWS_PER_PAGE
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val differ = object : DiffUtil.ItemCallback<List<CollectionItem>>() {
    override fun areItemsTheSame(oldItem: List<CollectionItem>, newItem: List<CollectionItem>): Boolean {
        if (oldItem.size != newItem.size) return false
        return oldItem.zip(newItem).all { (old, new) ->
            when {
                old is CollectionPodcast && new is CollectionPodcast ->
                    old.podcast.uuid == new.podcast.uuid

                old is CollectionHeader && new is CollectionHeader ->
                    old.title == new.title && old.subtitle == new.subtitle

                else -> false
            }
        }
    }

    override fun areContentsTheSame(oldItem: List<CollectionItem>, newItem: List<CollectionItem>): Boolean {
        return oldItem.toTypedArray().contentDeepEquals(newItem.toTypedArray())
    }
}

class CollectionListRowAdapter(
    val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit),
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    val onHeaderClicked: () -> Unit,
    val analyticsTracker: AnalyticsTracker,
) : ListAdapter<List<CollectionItem>, RecyclerView.ViewHolder>(differ) {

    private var fromListId: String? = null

    companion object {
        const val HEADER_OFFSET = 2
        private const val TYPE_HEADER = 0
        private const val TYPE_PODCAST = 1
    }

    class PodcastsViewHolder(
        val binding: ItemCollectionListBinding,
        onItemClicked: (Int, Int) -> Unit,
        onPodcastSubscribe: (Int, Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            const val NUMBER_OF_ROWS_PER_PAGE = 2
        }

        init {
            binding.row0.setOnClickListener {
                onItemClicked(bindingAdapterPosition, 0)
            }
            binding.row1.setOnClickListener {
                onItemClicked(bindingAdapterPosition, 1)
            }
            binding.row0.onSubscribeClicked = {
                onPodcastSubscribe(bindingAdapterPosition, 0)
            }
            binding.row1.onSubscribeClicked = {
                onPodcastSubscribe(bindingAdapterPosition, 1)
            }
        }

        val rows = listOf(binding.row0, binding.row1)

        fun bindPodcasts(podcastSublist: List<Any>) {
            rows.forEachIndexed { index, row ->
                row.isVisible = index < podcastSublist.size
                val podcast = podcastSublist.getOrNull(index) as? DiscoverPodcast
                row.podcast = podcast
            }
        }
    }

    class HeaderViewHolder(
        val binding: CollectionHeaderBinding,
        onHeaderClicked: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val imageRequestFactory = PocketCastsImageRequestFactory(
            binding.root.context,
            placeholderType = PlaceholderType.None,
        ).themed()

        init {
            binding.root.setOnClickListener {
                onHeaderClicked()
            }
        }

        fun bind(header: CollectionHeader) {
            binding.lblTitle.text = header.title
            binding.lblSubtitle.text = header.subtitle
            imageRequestFactory.createForFileOrUrl(header.imageUrl).loadInto(binding.imageHeader)
            binding.root.contentDescription =
                binding.root.context.getString(LR.string.discover_collection_header_content_description, header.title)
        }
    }

    fun submitPodcastList(list: List<CollectionItem>, header: CollectionItem?, commitCallback: Runnable?) {
        val chunkedList = list.chunked(NUMBER_OF_ROWS_PER_PAGE)
        val headerList = header?.let { listOf(listOf(it)) } ?: emptyList()

        submitList(headerList + chunkedList, commitCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        when (viewType) {
            TYPE_PODCAST -> {
                return PodcastsViewHolder(
                    ItemCollectionListBinding.inflate(inflater, parent, false),
                    onItemClicked = { pageIndex, podcastIndex ->
                        val items = getItem(pageIndex)
                        val podcasts = items.filterIsInstance<CollectionPodcast>().map { it.podcast }
                        val podcast = podcasts.getOrNull(podcastIndex) as? DiscoverPodcast

                        if (podcast == null) return@PodcastsViewHolder

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
                    onPodcastSubscribe = { pageIndex, podcastIndex ->
                        val items = getItem(pageIndex)
                        val podcasts = items.filterIsInstance<CollectionPodcast>().map { it.podcast }
                        val podcast = podcasts.getOrNull(podcastIndex) as? DiscoverPodcast
                        podcast?.let { onPodcastSubscribe(it, fromListId) }
                    },
                )
            }

            TYPE_HEADER -> {
                return HeaderViewHolder(CollectionHeaderBinding.inflate(inflater, parent, false)) {
                    onHeaderClicked()
                }
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val items = getItem(position) ?: emptyList()
        when (val collectionItem = items.firstOrNull()) {
            is CollectionHeader -> (holder as HeaderViewHolder).bind(collectionItem)
            is CollectionPodcast -> {
                val podcasts = items.filterIsInstance<CollectionPodcast>().map { it.podcast }
                (holder as PodcastsViewHolder).bindPodcasts(podcasts)
            }

            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemList: List<CollectionItem> = getItem(position) ?: emptyList()

        return when (itemList.firstOrNull()) {
            is CollectionHeader -> TYPE_HEADER
            is CollectionPodcast -> TYPE_PODCAST
            else -> throw IllegalArgumentException("Unknown type at position $position")
        }
    }

    fun setFromListId(value: String) {
        this.fromListId = value
    }

    fun getListId() = this.fromListId

    sealed class CollectionItem {
        data class CollectionHeader(val imageUrl: String, val title: String?, val subtitle: String?) : CollectionItem()
        data class CollectionPodcast(val podcast: DiscoverPodcast) : CollectionItem()
    }
}
