package au.com.shiftyjelly.pocketcasts.player.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextBinding
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextFooterBinding
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextPlayingBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.setEpisodeTimeLeft
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class UpNextAdapter(
    context: Context,
    val episodeManager: EpisodeManager,
    val listener: UpNextListener,
    val multiSelectHelper: MultiSelectEpisodesHelper,
    val fragmentManager: FragmentManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val upNextSource: UpNextSource,
    private val settings: Settings,
    private val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
    private val playbackManager: PlaybackManager,
) : ListAdapter<Any, RecyclerView.ViewHolder>(UPNEXT_ADAPTER_DIFF) {
    private val dateFormatter = RelativeDateFormatter(context)
    private val imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 3).themed()

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
            R.layout.adapter_up_next -> UpNextEpisodeViewHolder(
                binding = AdapterUpNextBinding.inflate(inflater, parent, false),
                listener = listener,
                dateFormatter = dateFormatter,
                imageRequestFactory = imageRequestFactory,
                episodeManager = episodeManager,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory,
                settings = settings,
            )
            R.layout.adapter_up_next_footer -> HeaderViewHolder(AdapterUpNextFooterBinding.inflate(inflater, parent, false))
            R.layout.adapter_up_next_playing -> PlayingViewHolder(AdapterUpNextPlayingBinding.inflate(inflater, parent, false))
            else -> throw IllegalStateException("Unknown view type in up next")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is BaseEpisode -> bindEpisodeRow(holder as UpNextEpisodeViewHolder, item)
            is PlayerViewModel.UpNextSummary -> (holder as HeaderViewHolder).bind(item)
            is UpNextPlaying -> (holder as PlayingViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is UpNextPlaying -> R.layout.adapter_up_next_playing
            is BaseEpisode -> R.layout.adapter_up_next
            is PlayerViewModel.UpNextSummary -> R.layout.adapter_up_next_footer
            else -> throw IllegalStateException("Unknown item type in up next")
        }
    }

    private fun bindEpisodeRow(holder: UpNextEpisodeViewHolder, episode: BaseEpisode) {
        holder.bind(
            episode = episode,
            isMultiSelecting = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(episode),
        )

        holder.binding.itemContainer.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
            } else {
                val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                val playOnTap = settings.tapOnUpNextShouldPlay.value
                trackUpNextEvent(AnalyticsEvent.UP_NEXT_QUEUE_EPISODE_TAPPED, mapOf(WILL_PLAY_KEY to playOnTap))
                listener.onEpisodeActionsClick(episodeUuid = episode.uuid, podcastUuid = podcastUuid)
            }
        }
        holder.binding.itemContainer.setOnLongClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = fragmentManager)
            } else {
                val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                val playOnLongPress = !settings.tapOnUpNextShouldPlay.value
                trackUpNextEvent(AnalyticsEvent.UP_NEXT_QUEUE_EPISODE_LONG_PRESSED, mapOf(WILL_PLAY_KEY to playOnLongPress))
                listener.onEpisodeActionsLongPress(episodeUuid = episode.uuid, podcastUuid = podcastUuid)
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
                btnClear.isEnabled = header.episodeCount > 0
                emptyUpNextContainer.isVisible = header.episodeCount == 0
                val time = TimeHelper.getTimeDurationShortString(timeMs = (header.totalTimeSecs * 1000).toLong(), context = root.context)
                btnClear.isVisible = playbackManager.getCurrentEpisode() != null
                lblUpNextTime.isVisible = playbackManager.getCurrentEpisode() != null
                lblUpNextTime.text = root.resources.getString(LR.string.player_up_next_time_remaining, time)
                root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    inner class PlayingViewHolder(val binding: AdapterUpNextPlayingBinding) : RecyclerView.ViewHolder(binding.root) {
        var loadedUuid: String? = null

        init {
            binding.root.setOnClickListener {
                trackUpNextEvent(AnalyticsEvent.UP_NEXT_NOW_PLAYING_TAPPED)
                listener.onNowPlayingClick()
            }
        }

        fun bind(playingState: UpNextPlaying) {
            Timber.d("Playing state episode: ${playingState.episode.playedUpTo}")
            binding.chapterProgress.theme = theme
            binding.chapterProgress.progress = playingState.progressPercent
            binding.chapterProgress.isVisible = playingState.progressPercent > 0
            binding.title.text = playingState.episode.title
            binding.downloaded.isVisible = playingState.episode.isDownloaded
            binding.info.setEpisodeTimeLeft(playingState.episode)
            binding.date.text = playingState.episode.getSummaryText(
                dateFormatter = dateFormatter,
                tintColor = ThemeColor.primaryText02(theme),
                showDuration = false,
                context = binding.date.context,
            )
            binding.reorder.imageTintList = ColorStateList.valueOf(ThemeColor.primaryInteractive01(theme))

            if (loadedUuid != playingState.episode.uuid) {
                imageRequestFactory.create(playingState.episode, settings.useEpisodeArtwork.value).loadInto(binding.image)
                loadedUuid = playingState.episode.uuid
            }

            binding.playingAnimation.apply {
                isVisible = isPlaying
                applyColorFilter(ThemeColor.primaryText01(theme))
            }
        }
    }

    private fun LottieAnimationView.applyColorFilter(@ColorInt color: Int) {
        val filter = SimpleColorFilter(color)
        val keyPath = KeyPath("**")
        val callback = LottieValueCallback<ColorFilter>(filter)
        addValueCallback(keyPath, LottieProperty.COLOR_FILTER, callback)
    }

    private fun trackUpNextEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        val properties = HashMap<String, Any>()
        properties[SOURCE_KEY] = upNextSource.analyticsValue
        properties.putAll(props)
        analyticsTracker.track(event, properties)
    }

    companion object {
        private const val SOURCE_KEY = "source"
        private const val WILL_PLAY_KEY = "will_play"
    }
}

data class UpNextPlaying(
    val episode: BaseEpisode,
    val progressPercent: Float,
)

private val UPNEXT_ADAPTER_DIFF = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is PlayerViewModel.UpNextSummary && newItem is PlayerViewModel.UpNextSummary) {
            true
        } else if (oldItem is BaseEpisode && newItem is BaseEpisode) {
            oldItem.uuid == newItem.uuid
        } else if (oldItem is UpNextPlaying && newItem is UpNextPlaying) {
            oldItem.episode.uuid == newItem.episode.uuid
        } else {
            false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is BaseEpisode && newItem is BaseEpisode) {
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
