package au.com.shiftyjelly.pocketcasts.discover.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowPromotionBinding
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPromotion
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import java.util.UUID

private val differ = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}

class PlainListAdapter(
    val onPodcastClick: (DiscoverPodcast) -> Unit,
    val onPodcastSubscribe: (String) -> Unit,
    val onPromotionClick: (DiscoverPromotion) -> Unit,
    val onEpisodeClick: (DiscoverEpisode) -> Unit,
    val onEpisodePlayClick: (DiscoverEpisode) -> Unit,
    val onEpisodeStopClick: () -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(differ) {

    var listTintColor: Int? = null

    init {
        // setting a stable id with getItemId stops the recyclerview rows flashing on update
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val uuid = when (val item = super.getItem(position)) {
            is DiscoverEpisode -> item.uuid
            is DiscoverPodcast -> item.uuid
            is DiscoverPromotion -> item.promotionUuid
            else -> null
        }
        return uuid?.let { UUID.fromString(uuid).mostSignificantBits } ?: super.getItemId(position)
    }

    inner class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val podcastRow: PodcastRow
            get() = this.itemView as PodcastRow

        fun bindPodcast(podcast: DiscoverPodcast) {
            podcastRow.podcast = podcast
            itemView.setOnClickListener {
                onPodcastClick(podcast)
            }
            podcastRow.onSubscribeClicked = {
                onPodcastSubscribe(podcast.uuid)
            }
        }
    }

    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val episodeRow: EpisodeRow
            get() = this.itemView as EpisodeRow

        fun bindEpisode(episode: DiscoverEpisode) {
            episodeRow.episode = episode
            itemView.setOnClickListener {
                onEpisodeClick(episode)
            }
            episodeRow.onPlayClicked = {
                episodeRow.episode = episode.copy(isPlaying = !episode.isPlaying)
                if (episode.isPlaying) {
                    onEpisodeStopClick()
                } else {
                    onEpisodePlayClick(episode)
                }
            }
        }
    }

    inner class PromotionViewHolder(private val binding: RowPromotionBinding) : RecyclerView.ViewHolder(binding.root) {

        private val imageLoader = PodcastImageLoaderThemed(itemView.context)

        fun bind(discoverPromotion: DiscoverPromotion) {
            binding.lblTitle.text = discoverPromotion.title
            binding.lblDescription.text = discoverPromotion.description
            imageLoader.loadSmallImage(discoverPromotion.podcastUuid).into(binding.imageView)

            binding.btnSubscribe.updateSubscribeButtonIcon(subscribed = discoverPromotion.isSubscribed)

            itemView.setOnClickListener {
                onPromotionClick(discoverPromotion)
            }
            binding.btnSubscribe.setOnClickListener {
                binding.btnSubscribe.updateSubscribeButtonIcon(subscribed = true)
                onPodcastSubscribe(discoverPromotion.podcastUuid)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DiscoverPodcast -> R.layout.row_podcast
            is DiscoverEpisode -> R.layout.row_episode
            is DiscoverPromotion -> R.layout.row_promotion
            else -> throw IllegalStateException("Unknown row type in discover list")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.row_podcast -> {
                val podcastRow = PodcastRow(parent.context)
                val sixteenDp = 16.dpToPx(parent.context)
                podcastRow.updatePadding(left = sixteenDp, right = sixteenDp)
                PodcastViewHolder(podcastRow)
            }
            R.layout.row_episode -> {
                val episodeRow = EpisodeRow(parent.context)
                episodeRow.listTintColor = listTintColor
                EpisodeViewHolder(episodeRow)
            }
            R.layout.row_promotion -> {
                PromotionViewHolder(RowPromotionBinding.inflate(inflater, parent, false))
            }
            else -> throw IllegalStateException("Unknown row type in discover list")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DiscoverPodcast -> (holder as PodcastViewHolder).bindPodcast(item)
            is DiscoverEpisode -> (holder as EpisodeViewHolder).bindEpisode(item)
            is DiscoverPromotion -> (holder as PromotionViewHolder).bind(item)
        }
    }
}
