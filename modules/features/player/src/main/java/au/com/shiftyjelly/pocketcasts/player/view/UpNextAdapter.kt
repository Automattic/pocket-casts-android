package au.com.shiftyjelly.pocketcasts.player.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
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
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.helper.setEpisodeTimeLeft
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class UpNextAdapter(
    context: Context,
    val episodeManager: EpisodeManager,
    val listener: UpNextListener,
    val multiSelectHelper: MultiSelectEpisodesHelper,
    val fragmentManager: FragmentManager,
    private val analyticsTracker: AnalyticsTracker,
    private val upNextSource: UpNextSource,
    private val settings: Settings,
    private val playbackManager: PlaybackManager,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val onSwipeAction: (BaseEpisode, SwipeAction) -> Unit,
) : ListAdapter<Any, RecyclerView.ViewHolder>(UPNEXT_ADAPTER_DIFF) {
    private val dateFormatter = RelativeDateFormatter(context)

    private val imageRequestFactory = PocketCastsImageRequestFactory(
        context,
        cornerRadius = 4,
    ).themed()

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

    private var isSignedInAsPaidUser: Boolean = false
    private var isUpNextNotEmpty: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_up_next -> {
                val binding = AdapterUpNextBinding.inflate(inflater, parent, false)
                UpNextEpisodeViewHolder(
                    binding = binding,
                    episodeManager = episodeManager,
                    imageRequestFactory = imageRequestFactory,
                    swipeRowActionsFactory = swipeRowActionsFactory,
                    listener = listener,
                    onRowClick = { episode ->
                        if (multiSelectHelper.isMultiSelecting) {
                            binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
                        } else {
                            val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                            val playOnTap = settings.tapOnUpNextShouldPlay.value
                            trackUpNextEvent(AnalyticsEvent.UP_NEXT_QUEUE_EPISODE_TAPPED, mapOf(WILL_PLAY_KEY to playOnTap))
                            listener.onEpisodeActionsClick(episodeUuid = episode.uuid, podcastUuid = podcastUuid)
                        }
                    },
                    onRowLongClick = { episode ->
                        if (multiSelectHelper.isMultiSelecting) {
                            multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = fragmentManager)
                        } else {
                            val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                            val playOnLongPress = !settings.tapOnUpNextShouldPlay.value
                            trackUpNextEvent(AnalyticsEvent.UP_NEXT_QUEUE_EPISODE_LONG_PRESSED, mapOf(WILL_PLAY_KEY to playOnLongPress))
                            listener.onEpisodeActionsLongPress(episodeUuid = episode.uuid, podcastUuid = podcastUuid)
                        }
                    },
                    onSwipeAction = onSwipeAction,
                )
            }

            R.layout.adapter_up_next_footer -> HeaderViewHolder(AdapterUpNextFooterBinding.inflate(inflater, parent, false))
            R.layout.adapter_up_next_playing -> PlayingViewHolder(AdapterUpNextPlayingBinding.inflate(inflater, parent, false))
            else -> throw IllegalStateException("Unknown view type in up next")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is BaseEpisode -> bindEpisodeRow(holder as UpNextEpisodeViewHolder, item, animateMultiSelection = false)
            is PlayerViewModel.UpNextSummary -> (holder as HeaderViewHolder).bind(item)
            is UpNextPlaying -> (holder as PlayingViewHolder).bind(item)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any?>) {
        when (val item = getItem(position)) {
            is BaseEpisode -> bindEpisodeRow(holder as UpNextEpisodeViewHolder, item, animateMultiSelection = MULTI_SELECT_TOGGLE_PAYLOAD in payloads)
            else -> super.onBindViewHolder(holder, position, payloads)
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

    private fun bindEpisodeRow(
        holder: UpNextEpisodeViewHolder,
        episode: BaseEpisode,
        animateMultiSelection: Boolean,
    ) {
        holder.bind(
            episode = episode,
            isMultiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(episode),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.UpNext),
            animateMultiSelection = animateMultiSelection,
        )
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? UpNextEpisodeViewHolder)?.unbind()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as? UpNextEpisodeViewHolder)?.unbind()
    }

    fun updateUserSignInState(isSignedInAsPaidUser: Boolean) {
        this.isSignedInAsPaidUser = isSignedInAsPaidUser
    }

    fun updateUpNextEmptyState(isUpNextNotEmpty: Boolean) {
        this.isUpNextNotEmpty = isUpNextNotEmpty
    }

    inner class HeaderViewHolder(val binding: AdapterUpNextFooterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: PlayerViewModel.UpNextSummary) {
            with(binding) {
                binding.emptyUpNextComposeView.setContentWithViewCompositionStrategy {
                    AppTheme(theme) {
                        if (header.episodePlaying && header.episodeCount == 0) {
                            UpNextNoContentBanner(
                                onDiscoverClick = listener::onDiscoverTapped,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                }

                val time = TimeHelper.getTimeDurationShortString(timeMs = (header.totalTimeSecs * 1000).toLong(), context = root.context)
                lblUpNextTime.isVisible = hasEpisodeInProgress()
                lblUpNextTime.text = if (header.episodeCount == 0) {
                    root.resources.getString(LR.string.player_up_next_time_left, time)
                } else {
                    root.resources.getQuantityString(LR.plurals.player_up_next_header_title, header.episodeCount, header.episodeCount, time)
                }

                shuffle.isVisible = isUpNextNotEmpty
                shuffle.updateShuffleButton()

                shuffle.setOnClickListener {
                    if (isSignedInAsPaidUser) {
                        val newValue = !settings.upNextShuffle.value
                        analyticsTracker.track(AnalyticsEvent.UP_NEXT_SHUFFLE_ENABLED, mapOf("value" to newValue, SOURCE_KEY to upNextSource.analyticsValue))

                        if (newValue) {
                            (root.context.getActivity() as? FragmentHostListener)?.snackBarView()?.let { snackBarView ->
                                Snackbar.make(snackBarView, root.resources.getString(LR.string.up_next_shuffle_enable_confirmation_message), Snackbar.LENGTH_LONG).show()
                            }
                        }

                        settings.upNextShuffle.set(newValue, updateModifiedAt = false)
                    } else {
                        OnboardingLauncher.openOnboardingFlow(
                            requireNotNull(root.context.getActivity()),
                            OnboardingFlow.Upsell(OnboardingUpgradeSource.UP_NEXT_SHUFFLE),
                        )
                    }

                    shuffle.updateShuffleButton()
                }

                root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        private fun ImageButton.updateShuffleButton() {
            this.setImageResource(
                when {
                    !isSignedInAsPaidUser -> IR.drawable.shuffle_plus_feature_icon
                    settings.upNextShuffle.value -> IR.drawable.shuffle_enabled
                    else -> IR.drawable.shuffle
                },
            )

            this.contentDescription = context.getString(
                when {
                    isSignedInAsPaidUser -> LR.string.up_next_shuffle_button_content_description
                    settings.upNextShuffle.value -> LR.string.up_next_shuffle_disable_button_content_description
                    else -> LR.string.up_next_shuffle_button_content_description
                },
            )

            if (isSignedInAsPaidUser) {
                this.setImageTintList(
                    ColorStateList.valueOf(
                        if (settings.upNextShuffle.value) ThemeColor.primaryIcon01(theme) else ThemeColor.primaryIcon02(theme),
                    ),
                )
            }

            TooltipCompat.setTooltipText(
                this,
                context.getString(LR.string.up_next_shuffle_button_content_description),
            )
        }

        private fun hasEpisodeInProgress() = playbackManager.getCurrentEpisode() != null
    }

    inner class PlayingViewHolder(val binding: AdapterUpNextPlayingBinding) : RecyclerView.ViewHolder(binding.root) {
        private var loadedUuid: String? = null
        private val cardCornerRadius: Float = 4.dpToPx(itemView.context.resources.displayMetrics).toFloat()
        private val cardElevation: Float = 2.dpToPx(itemView.context.resources.displayMetrics).toFloat()

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
            binding.title.text = playingState.episode.title
            binding.downloaded.isVisible = playingState.episode.isDownloaded
            binding.info.setEpisodeTimeLeft(playingState.episode)
            binding.date.text = playingState.episode.getSummaryText(
                dateFormatter = dateFormatter,
                tintColor = ThemeColor.primaryText02(theme),
                showDuration = false,
                context = binding.date.context,
            )
            binding.reorder.imageTintList = ColorStateList.valueOf(ThemeColor.primaryText01(theme))

            if (loadedUuid != playingState.episode.uuid) {
                imageRequestFactory.create(playingState.episode, settings.artworkConfiguration.value.useEpisodeArtwork(Element.UpNext)).loadInto(binding.image)
                loadedUuid = playingState.episode.uuid
            }

            binding.playingAnimation.apply {
                isVisible = isPlaying
                applyColorFilter(ThemeColor.primaryText01(theme))
            }

            binding.imageCardView.radius = cardCornerRadius
            binding.imageCardView.elevation = cardElevation
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
