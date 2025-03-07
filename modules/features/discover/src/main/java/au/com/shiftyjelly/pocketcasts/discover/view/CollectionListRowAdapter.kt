package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.view.CollectionListRowAdapter.CollectionListViewHolder.Companion.NUMBER_OF_ROWS_PER_PAGE
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

internal class CollectionListRowAdapter(
    val onPodcastClicked: ((DiscoverPodcast, String?) -> Unit),
    val onPodcastSubscribe: (DiscoverPodcast, String?) -> Unit,
    val analyticsTracker: AnalyticsTracker,
) : ListAdapter<List<Any>, CollectionListRowAdapter.CollectionListViewHolder>(SmallListDiffer) {
    class CollectionListViewHolder(val binding: ItemCollectionListBinding) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            const val NUMBER_OF_ROWS_PER_PAGE = 2
        }

        val rows = listOf(binding.row0, binding.row1)
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
        return CollectionListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionListViewHolder, position: Int) {
        val podcastSublist = getItem(position)
        holder.rows.forEach {
            it.clear()
        }

        podcastSublist.forEachIndexed { index, podcast ->
            val podcastRow = holder.rows[index]
            if (podcast is DiscoverPodcast) {
                podcastRow.podcast = podcast
                podcastRow.isClickable = true
                podcastRow.setOnClickListener {
                    fromListId?.let {
                        analyticsTracker.track(
                            AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
                            mapOf(
                                DiscoverFragment.Companion.LIST_ID_KEY to it,
                                DiscoverFragment.Companion.PODCAST_UUID_KEY to podcast.uuid,
                            ),
                        )
                    }
                    onPodcastClicked(podcast, fromListId)
                }
                podcastRow.onSubscribeClicked = {
                    fromListId?.let {
                        analyticsTracker.track(
                            AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
                            mapOf(
                                DiscoverFragment.Companion.LIST_ID_KEY to it, DiscoverFragment.Companion.PODCAST_UUID_KEY to podcast.uuid,
                            ),
                        )
                    }
                    onPodcastSubscribe(podcast, fromListId)
                }
            } else {
                podcastRow.podcast = null
                podcastRow.isClickable = false
            }
        }
    }
    fun setFromListId(value: String) {
        this.fromListId = value
    }
}
