package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionListViewHolder.Companion.NUMBER_OF_ROWS_PER_PAGE
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

internal class CollectionListRowAdapter(
    val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit),
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    val analyticsTracker: AnalyticsTracker,
) : ListAdapter<List<Any>, CollectionListRowAdapter.CollectionListViewHolder>(SmallListDiffer) {

    class CollectionListViewHolder(val binding: ItemCollectionListBinding, onItemClicked: (Int, Int) -> Unit) : RecyclerView.ViewHolder(binding.root) {

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
        }

        val rows = listOf(binding.row0, binding.row1)

        fun bind(podcastSublist: List<Any>) {
            rows.forEachIndexed { index, row ->
                row.clear()
                row.isVisible = index < podcastSublist.size

                val podcast = podcastSublist.getOrNull(index) as? DiscoverPodcast
                row.podcast = podcast
            }
        }
    }

    private var fromListId: String? = null

    fun submitPodcastList(list: List<DiscoverPodcast>, commitCallback: Runnable?) {
        submitList(list.chunked(NUMBER_OF_ROWS_PER_PAGE), commitCallback)
    }

    fun showLoadingList() {
        val loadingList = listOf(MutableList(NUMBER_OF_ROWS_PER_PAGE) { LoadingItem() })
        submitList(loadingList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCollectionListBinding.inflate(inflater, parent, false)

        return CollectionListViewHolder(binding) { pageIndex, podcastIndex ->
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
        }
    }

    override fun onBindViewHolder(holder: CollectionListViewHolder, position: Int) {
        val podcastSublist = getItem(position)
        holder.bind(podcastSublist)
    }

    fun setFromListId(value: String) {
        this.fromListId = value
    }
}
