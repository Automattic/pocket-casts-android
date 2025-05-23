package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.discoverListPodcastSubscribed
import au.com.shiftyjelly.pocketcasts.analytics.discoverListPodcastTapped
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemSmallListBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastList
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

val SmallListDiffer = object : DiffUtil.ItemCallback<List<Any>>() {
    override fun areItemsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
        return oldItem.filterIsInstance(DiscoverPodcast::class.java)
            .map { it.uuid }
            .toTypedArray()
            .contentDeepEquals(
                newItem
                    .filterIsInstance(DiscoverPodcast::class.java)
                    .map { it.uuid }.toTypedArray(),
            ) // Checks if the list is of the same podcasts in the same order
    }

    override fun areContentsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
        // The small list row is built of lists of NUMBER_OF_ROWS_PER_PAGE podcasts. We need to diff if any of the podcasts
        // in each list have changed.
        return oldItem.filterIsInstance(DiscoverPodcast::class.java).toTypedArray().contentDeepEquals(newItem.filterIsInstance(DiscoverPodcast::class.java).toTypedArray())
    }
}

internal class SmallListRowAdapter(
    val onPodcastClicked: ((DiscoverPodcast, String?, String?) -> Unit),
    val onPodcastSubscribe: (DiscoverPodcast, String?, String?) -> Unit,
    val analyticsTracker: AnalyticsTracker,
) : ListAdapter<List<Any>, SmallListRowAdapter.SmallListViewHolder>(SmallListDiffer) {
    class SmallListViewHolder(val binding: ItemSmallListBinding) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            const val NUMBER_OF_ROWS_PER_PAGE = 4
        }

        val rows = listOf(binding.row0, binding.row1, binding.row2, binding.row3)
    }

    var list: PodcastList? = null

    fun submitPodcastList(list: List<DiscoverPodcast>, commitCallback: Runnable?) {
        submitList(list.chunked(SmallListViewHolder.NUMBER_OF_ROWS_PER_PAGE), commitCallback)
    }

    fun showLoadingList() {
        val loadingList = listOf(MutableList(SmallListViewHolder.NUMBER_OF_ROWS_PER_PAGE) { LoadingItem() })
        submitList(loadingList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSmallListBinding.inflate(inflater, parent, false)
        return SmallListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SmallListViewHolder, position: Int) {
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
                    analyticsTracker.discoverListPodcastTapped(podcastUuid = podcast.uuid, listId = list?.listId, listDate = list?.date)
                    onPodcastClicked(podcast, list?.listId, list?.date)
                }
                podcastRow.onSubscribeClicked = {
                    analyticsTracker.discoverListPodcastSubscribed(podcastUuid = podcast.uuid, listId = list?.listId, listDate = list?.date)
                    onPodcastSubscribe(podcast, list?.listId, list?.date)
                }
            } else {
                podcastRow.podcast = null
                podcastRow.isClickable = false
            }
        }
    }
}
