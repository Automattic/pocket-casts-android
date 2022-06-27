package au.com.shiftyjelly.pocketcasts.player.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextFooterBinding
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextPlayingBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class UpNextAdapter(context: Context, val imageLoader: PodcastImageLoader, val episodeManager: EpisodeManager, val listener: UpNextListener, val multiSelectHelper: MultiSelectHelper, val fragmentManager: FragmentManager) : ListAdapter<Any, RecyclerView.ViewHolder>(UPNEXT_ADAPTER_DIFF) {
    private val dateFormatter = RelativeDateFormatter(context)

    var isPlaying: Boolean = false
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    var theme: Theme.ThemeType = Theme.ThemeType.DARK
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_up_next -> UpNextEpisodeViewHolder(DataBindingUtil.inflate(inflater, R.layout.adapter_up_next, parent, false), listener, dateFormatter, imageLoader, episodeManager)
            R.layout.adapter_up_next_footer -> HeaderViewHolder(DataBindingUtil.inflate(inflater, R.layout.adapter_up_next_footer, parent, false))
            R.layout.adapter_up_next_playing -> PlayingViewHolder(DataBindingUtil.inflate(inflater, R.layout.adapter_up_next_playing, parent, false))
            else -> throw IllegalStateException("Unknown view type in up next")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is Playable -> bindPlayableRow(holder as UpNextEpisodeViewHolder, item)
            is PlayerViewModel.UpNextSummary -> (holder as HeaderViewHolder).bind(item)
            is UpNextPlaying -> (holder as PlayingViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is UpNextPlaying -> R.layout.adapter_up_next_playing
            is Playable -> R.layout.adapter_up_next
            is PlayerViewModel.UpNextSummary -> R.layout.adapter_up_next_footer
            else -> throw IllegalStateException("Unknown item type in up next")
        }
    }

    private fun bindPlayableRow(holder: UpNextEpisodeViewHolder, item: Playable) {
        holder.bind(item, multiSelectHelper.isMultiSelecting, multiSelectHelper.isSelected(item))

        holder.binding.itemContainer.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(item)
            } else {
                val podcastUuid = (item as? Episode)?.podcastUuid
                listener.onEpisodeActionsClick(episodeUuid = item.uuid, podcastUuid = podcastUuid)
            }
        }
        holder.binding.itemContainer.setOnLongClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                multiSelectHelper.defaultLongPress(episode = item, fragmentManager = fragmentManager)
            } else {
                val podcastUuid = (item as? Episode)?.podcastUuid
                listener.onEpisodeActionsLongPress(episodeUuid = item.uuid, podcastUuid = podcastUuid)
            }
            true
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? UpNextEpisodeViewHolder)?.clearDisposable()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as? UpNextEpisodeViewHolder)?.clearDisposable()
    }

    inner class HeaderViewHolder(val binding: AdapterUpNextFooterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnClear.setOnClickListener { listener.onClearUpNext() }
        }

        fun bind(header: PlayerViewModel.UpNextSummary) {
            with(binding) {
                episodeCount = header.episodeCount
                val time = TimeHelper.getTimeDurationShortString(timeMs = (header.totalTimeSecs * 1000).toLong(), context = root.context)
                totalTime = root.resources.getString(LR.string.player_up_next_time_remaining, time)
                root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                executePendingBindings()
            }
        }
    }

    inner class PlayingViewHolder(val binding: AdapterUpNextPlayingBinding) : RecyclerView.ViewHolder(binding.root) {
        var loadedUuid: String? = null

        init {
            binding.root.setOnClickListener { listener.onNowPlayingClick() }
        }

        fun bind(playingState: UpNextPlaying) {
            val titleColor = ThemeColor.playerContrast01(theme)
            val tintColor = ThemeColor.playerContrast02(theme)

            Timber.d("Playing state episode: ${playingState.episode.playedUpTo}")
            binding.chapterProgress.theme = theme
            binding.title.setTextColor(titleColor)
            binding.info.setTextColor(tintColor)
            binding.playingState = playingState
            binding.date.text = playingState.episode.getSummaryText(dateFormatter = dateFormatter, tintColor = tintColor, showDuration = false, context = binding.date.context)
            binding.reorder.imageTintList = ColorStateList.valueOf(ThemeColor.playerContrast01(theme))
            binding.executePendingBindings()

            if (loadedUuid != playingState.episode.uuid) {
                imageLoader.radiusPx = 3.dpToPx(itemView.context)
                imageLoader.load(playingState.episode).into(binding.image)
                loadedUuid = playingState.episode.uuid
            }

            binding.playingAnimation.isVisible = isPlaying
        }
    }
}

data class UpNextPlaying(
    val episode: Playable,
    val progressPercent: Float
)

private val UPNEXT_ADAPTER_DIFF = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is PlayerViewModel.UpNextSummary && newItem is PlayerViewModel.UpNextSummary) {
            true
        } else if (oldItem is Playable && newItem is Playable) {
            oldItem.uuid == newItem.uuid
        } else if (oldItem is UpNextPlaying && newItem is UpNextPlaying) {
            oldItem.episode.uuid == newItem.episode.uuid
        } else {
            false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is Playable && newItem is Playable) {
            oldItem.uuid == newItem.uuid &&
                oldItem.title == newItem.title &&
                oldItem.publishedDate == newItem.publishedDate &&
                oldItem.duration == newItem.duration &&
                oldItem.playedUpTo == newItem.playedUpTo &&
                oldItem.episodeStatus == newItem.episodeStatus
        } else if (oldItem is UpNextPlaying && newItem is UpNextPlaying) {
            oldItem.episode.uuid == newItem.episode.uuid &&
                oldItem.progressPercent == newItem.progressPercent &&
                oldItem.episode.playedUpTo == newItem.episode.playedUpTo
        } else {
            oldItem == newItem
        }
    }
}
