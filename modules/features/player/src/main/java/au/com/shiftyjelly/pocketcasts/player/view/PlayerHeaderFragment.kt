package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.LayoutTransition
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.binding.playIfTrue
import au.com.shiftyjelly.pocketcasts.player.binding.setSeekBarState
import au.com.shiftyjelly.pocketcasts.player.binding.showIfPresent
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterPlayerHeaderBinding
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptPageWrapper
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptSearchViewModel
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.ShelfItemSource
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.SnackbarMessage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.openUrl
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.component.LockableNestedScrollView
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType.Danger
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val UP_NEXT_FLING_VELOCITY_THRESHOLD = 1000.0f

@AndroidEntryPoint
class PlayerHeaderFragment : BaseFragment(), PlayerClickListener {
    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var warningsHelper: WarningsHelper

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var bookmarkFeature: BookmarkFeatureControl

    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory
    private val viewModel: PlayerViewModel by activityViewModels()
    private val shelfSharedViewModel: ShelfSharedViewModel by activityViewModels()
    private val transcriptViewModel by viewModels<TranscriptViewModel>({ requireParentFragment() })
    private val transcriptSearchViewModel by viewModels<TranscriptSearchViewModel>({ requireParentFragment() })
    private var binding: AdapterPlayerHeaderBinding? = null
    private val sourceView = SourceView.PLAYER

    private val activityLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(BookmarkActivityContract()) { result ->
        showViewBookmarksSnackbar(result)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = AdapterPlayerHeaderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        imageRequestFactory = PocketCastsImageRequestFactory(view.context, cornerRadius = 8).themed().copy(isDarkTheme = true)

        binding.playerControls.initPlayerControls(
            ::onSkipBack,
            ::onSkipForward,
            ::onSkipForwardLongPress,
            ::onPlayClicked,
        )

        binding.seekBar.changeListener = object : PlayerSeekBar.OnUserSeekListener {
            override fun onSeekPositionChangeStop(progress: Duration, seekComplete: () -> Unit) {
                val progressMs = progress.inWholeMilliseconds.toInt()
                viewModel.seekToMs(progressMs, seekComplete)
                playbackManager.trackPlaybackSeek(progressMs, SourceView.PLAYER)
            }

            override fun onSeekPositionChanging(progress: Duration) {}

            override fun onSeekPositionChangeStart() {
            }
        }

        val shelfViews = mapOf(
            ShelfItem.Effects to binding.effects,
            ShelfItem.Sleep to binding.sleep,
            ShelfItem.Star to binding.star,
            ShelfItem.Share to binding.share,
            ShelfItem.Podcast to binding.podcast,
            ShelfItem.Cast to binding.cast,
            ShelfItem.Played to binding.played,
            ShelfItem.Archive to binding.archive,
            ShelfItem.Bookmark to binding.bookmark,
            ShelfItem.Transcript to binding.transcript,
            ShelfItem.Download to binding.download,
            ShelfItem.Report to binding.report,
        )
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.uiState.collect { uiState ->
                    binding.shelf.removeAllViews()
                    uiState.shelfItems.take(4)
                        .mapNotNull(shelfViews::get)
                        .forEach { itemView -> binding.shelf += itemView }
                    binding.shelf.addView(binding.playerActions)
                }
            }
        }

        if (FeatureFlag.isEnabled(Feature.TRANSCRIPTS)) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    transcriptViewModel.uiState.collect { uiState ->
                        val transcriptAvailable = uiState !is TranscriptViewModel.UiState.Empty
                        binding.transcript.alpha = if (transcriptAvailable) 1f else 0.4f
                        binding.transcript.setOnClickListener {
                            if (!transcriptAvailable) {
                                val message = getString(LR.string.transcript_error_not_available)
                                Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                                    .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.LIGHT))
                                    .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.LIGHT))
                                    .show()
                            } else {
                                shelfSharedViewModel.openTranscript(ShelfItemSource.Shelf)
                            }
                        }
                    }
                }
            }
        }

        observeShelfItemNavigationState()
        observeShelfItemSnackbarMessages()

        binding.effects.setOnClickListener {
            shelfSharedViewModel.onEffectsClick(ShelfItemSource.Shelf)
        }
        binding.previousChapter.setOnClickListener { onPreviousChapter() }
        binding.nextChapter.setOnClickListener { onNextChapter() }
        binding.sleep.setOnClickListener {
            shelfSharedViewModel.onSleepClick(ShelfItemSource.Shelf)
        }
        binding.star.setOnClickListener {
            shelfSharedViewModel.onStarClick(ShelfItemSource.Shelf)
        }
        binding.share.setOnClickListener {
            val podcast = viewModel.podcast ?: return@setOnClickListener
            val episode = viewModel.episode as? PodcastEpisode ?: return@setOnClickListener
            shelfSharedViewModel.onShareClick(podcast, episode, ShelfItemSource.Shelf)
        }
        binding.playerActions.setOnClickListener { shelfSharedViewModel.onMoreClick() }
        binding.podcast.setOnClickListener {
            shelfSharedViewModel.onShowPodcastOrCloudFiles(viewModel.podcast, ShelfItemSource.Shelf)
        }
        binding.played.setOnClickListener {
            shelfSharedViewModel.onPlayedClick(
                onMarkAsPlayedConfirmed = { episode, shouldShuffleUpNext ->
                    viewModel.markAsPlayedConfirmed(episode, shouldShuffleUpNext)
                },
                source = ShelfItemSource.Shelf,
            )
        }
        binding.archive.setOnClickListener {
            shelfSharedViewModel.onArchiveClick(
                onArchiveConfirmed = { viewModel.archiveConfirmed(it) },
                source = ShelfItemSource.Shelf,
            )
        }
        binding.bookmark.setOnClickListener {
            shelfSharedViewModel.onAddBookmarkClick(
                OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                ShelfItemSource.Shelf,
            )
        }
        binding.report?.setOnClickListener {
            shelfSharedViewModel.onReportClick(ShelfItemSource.Shelf)
        }
        binding.download.setOnClickListener {
            viewModel.handleDownloadClickFromPlaybackActions(
                onDownloadStart = { shelfSharedViewModel.onEpisodeDownloadStart(ShelfItemSource.Shelf) },
                onDeleteStart = { shelfSharedViewModel.onEpisodeRemoveClick(ShelfItemSource.Shelf) },
            )
        }
        binding.videoView.playbackManager = playbackManager
        binding.videoView.setOnClickListener { onFullScreenVideoClick() }

        with(binding.castButton) {
            CastButtonFactory.setUpMediaRouteButton(binding.root.context, this)
            visibility = View.VISIBLE
            updateColor(ThemeColor.playerContrast03(theme.activeTheme))
            setOnClickListener {
                shelfSharedViewModel.trackShelfAction(ShelfItem.Cast, ShelfItemSource.Shelf)
            }
        }

        if (FeatureFlag.isEnabled(Feature.TRANSCRIPTS)) {
            setupTranscriptPage()
            observeTranscriptPageTransition()
        }

        setupUpNextDrag(binding)

        viewModel.listDataLive.observe(viewLifecycleOwner) {
            val headerViewModel = it.podcastHeader
            val playerContrast1 = ThemeColor.playerContrast01(headerViewModel.theme)

            binding.seekBar.setSeekBarState(
                duration = headerViewModel.durationMs.milliseconds,
                position = headerViewModel.positionMs.milliseconds,
                chapters = headerViewModel.chapters,
                playbackSpeed = headerViewModel.playbackEffects.playbackSpeed,
                adjustDuration = headerViewModel.adjustRemainingTimeDuration,
                tintColor = headerViewModel.iconTintColor,
                bufferedUpTo = headerViewModel.bufferedUpToMs,
                isBuffering = headerViewModel.isBuffering,
                theme = headerViewModel.theme,
            )
            binding.playerControls.updatePlayerControls(headerViewModel, playerContrast1)

            binding.episodeTitle.setTextColor(playerContrast1)

            headerViewModel.episode?.let { episode ->
                loadArtwork(episode, headerViewModel.useEpisodeArtwork, binding.artwork)

                val isPodcast = episode is PodcastEpisode

                val downloadIcon = when {
                    isPodcast && (episode.isDownloading || episode.isQueued) -> IR.drawable.ic_download
                    isPodcast && episode.isDownloaded -> IR.drawable.ic_downloaded_24dp
                    else -> IR.drawable.ic_download
                }

                binding.download.apply {
                    setImageResource(downloadIcon)

                    contentDescription = when {
                        isPodcast && (episode.isDownloading || episode.isQueued) -> context.getString(LR.string.episode_downloading)
                        isPodcast && episode.isDownloaded -> context.getString(LR.string.remove_downloaded_file)
                        else -> context.getString(LR.string.download)
                    }

                    layoutParams = layoutParams?.apply {
                        width = 0.dpToPx(context)
                        height = 48.dpToPx(context)
                    }

                    scaleType = ImageView.ScaleType.CENTER

                    val padding = 12.dpToPx(context)
                    setPadding(padding, padding, padding, padding)
                }
            }

            binding.podcastTitle.setOnClickListener {
                val podcastUuid = headerViewModel.podcastUuid ?: return@setOnClickListener
                analyticsTracker.track(
                    AnalyticsEvent.EPISODE_DETAIL_PODCAST_NAME_TAPPED,
                    mapOf(
                        AnalyticsProp.Key.EPISODE_UUID to headerViewModel.episodeUuid,
                        AnalyticsProp.Key.SOURCE to EpisodeViewSource.NOW_PLAYING.value,
                    ),
                )
                (activity as? FragmentHostListener)?.let { listener ->
                    listener.closePlayer()
                    listener.openPodcastPage(podcastUuid, sourceView.analyticsValue)
                }
            }

            loadChapterArtwork(headerViewModel.chapter, binding.chapterArtwork)
            binding.videoView.show = headerViewModel.isVideo
            binding.videoView.updatePlayerPrepared(headerViewModel.isPrepared)

            val highlightTint = playerHighlightColor(headerViewModel)
            val unhighlightTint = ThemeColor.playerContrast03(headerViewModel.theme)

            val effectTint = if (headerViewModel.isEffectsOn) highlightTint else unhighlightTint
            val effectResource = if (headerViewModel.isEffectsOn) R.drawable.ic_effects_on_32 else R.drawable.ic_effects_off_32
            binding.effects.setImageResource(effectResource)
            binding.effects.imageTintList = ColorStateList.valueOf(effectTint)

            val starTint = if (headerViewModel.isStarred) highlightTint else unhighlightTint
            val starResource = if (headerViewModel.isStarred) R.drawable.ic_star_filled_32 else R.drawable.ic_star_32
            binding.star.setImageResource(starResource)
            binding.star.imageTintList = ColorStateList.valueOf(starTint)

            if (headerViewModel.isChaptersPresent) {
                headerViewModel.chapter?.let {
                    binding.episodeTitle.setOnClickListener {
                        onHeaderChapterClick(headerViewModel.chapter)
                    }
                }
            } else {
                binding.episodeTitle.setOnClickListener(null)
            }

            binding.share.isVisible = !headerViewModel.isUserEpisode
            binding.star.isVisible = !headerViewModel.isUserEpisode

            val sleepTint = if (headerViewModel.isSleepRunning) highlightTint else unhighlightTint
            binding.sleep.alpha = if (headerViewModel.isSleepRunning) 1F else Color.alpha(sleepTint) / 255F * 2F
            binding.sleep.post { // this only works the second time it's called unless it's in a post
                binding.sleep.addValueCallback(KeyPath("**"), LottieProperty.COLOR, { sleepTint })
            }

            if (headerViewModel.chapter != null) {
                binding.chapterUrl.setOnClickListener {
                    headerViewModel.chapter.url?.let {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(it.toString())
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            UiUtil.displayAlertError(requireContext(), getString(LR.string.player_open_url_failed, it), null)
                        }
                    }
                }
            } else {
                binding.chapterUrl.setOnClickListener(null)
            }

            binding.archive.setImageResource(if (headerViewModel.isUserEpisode) R.drawable.ic_delete_32 else R.drawable.ic_archive_32)
            binding.chapterProgressCircle.progress = headerViewModel.chapterProgress
            binding.chapterProgressCircle.isVisible = headerViewModel.isChaptersPresent
            binding.chapterTimeRemaining.text = headerViewModel.chapterTimeRemaining

            binding.playerGroup.setBackgroundColor(headerViewModel.backgroundColor)
            binding.artwork.isVisible = headerViewModel.isPodcastArtworkVisible()
            binding.chapterArtwork.isVisible = headerViewModel.isChapterArtworkVisible()
            binding.chapterUrl.showIfPresent(headerViewModel.chapter?.url)
            binding.chapterUrlFront?.showIfPresent(headerViewModel.chapter?.url)
            binding.videoView.isVisible = headerViewModel.isVideoVisible()
            binding.episodeTitle.text = headerViewModel.title
            binding.podcastTitle.text = headerViewModel.podcastTitle
            binding.podcastTitle.isVisible = headerViewModel.podcastTitle?.isNotBlank() == true
            binding.chapterSummary.text = headerViewModel.chapterSummary
            binding.chapterSummary.isVisible = headerViewModel.isChaptersPresent
            binding.previousChapter.alpha = if (headerViewModel.isFirstChapter) 0.5f else 1f
            binding.previousChapter.isEnabled = !headerViewModel.isFirstChapter
            binding.previousChapter.isVisible = headerViewModel.isChaptersPresent
            binding.nextChapter.alpha = if (headerViewModel.isLastChapter) 0.5f else 1f
            binding.nextChapter.isEnabled = !headerViewModel.isLastChapter
            binding.nextChapter.isVisible = headerViewModel.isChaptersPresent
            binding.seekBar.setSeekBarState(
                duration = headerViewModel.durationMs.milliseconds,
                position = headerViewModel.positionMs.milliseconds,
                chapters = headerViewModel.chapters,
                playbackSpeed = headerViewModel.playbackEffects.playbackSpeed,
                adjustDuration = headerViewModel.adjustRemainingTimeDuration,
                tintColor = headerViewModel.iconTintColor,
                bufferedUpTo = headerViewModel.bufferedUpToMs,
                isBuffering = headerViewModel.isBuffering,
                theme = headerViewModel.theme,
            )
            binding.sleep.playIfTrue(headerViewModel.isSleepRunning)
        }
    }

    private fun observeShelfItemSnackbarMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.snackbarMessages.collect { message ->
                    val text = when (message) {
                        SnackbarMessage.EpisodeDownloadStarted -> LR.string.episode_queued_for_download
                        SnackbarMessage.EpisodeRemoved -> LR.string.episode_was_removed
                        SnackbarMessage.TranscriptNotAvailable -> LR.string.transcript_error_not_available
                    }
                    showSnackBar(text = getString(text))
                }
            }
        }
    }

    private fun observeShelfItemNavigationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.navigationState.collect { navigationState ->
                    when (navigationState) {
                        NavigationState.ShowEffectsOption -> {
                            EffectsFragment().show(parentFragmentManager, "effects")
                        }

                        NavigationState.ShowSleepTimerOptions -> {
                            SleepFragment().show(parentFragmentManager, "sleep_sheet")
                        }

                        is NavigationState.ShowShareDialog -> {
                            ShareDialogFragment
                                .newThemedInstance(navigationState.podcast, navigationState.episode, theme, SourceView.PLAYER)
                                .show(parentFragmentManager, "share_dialog")
                        }

                        is NavigationState.ShowPodcast -> {
                            (activity as FragmentHostListener).closePlayer()
                            (activity as? FragmentHostListener)?.openPodcastPage(navigationState.podcast.uuid, SourceView.PLAYER.analyticsValue)
                        }

                        is NavigationState.ShowCloudFiles -> {
                            (activity as FragmentHostListener).closePlayer()
                            (activity as? FragmentHostListener)?.openCloudFiles()
                        }

                        is NavigationState.ShowMarkAsPlayedConfirmation -> {
                            context?.let {
                                ConfirmationDialog()
                                    .setForceDarkTheme(true)
                                    .setSummary(it.getString(LR.string.player_mark_as_played))
                                    .setIconId(IR.drawable.ic_markasplayed)
                                    .setButtonType(Danger(it.getString(LR.string.player_mark_as_played_button)))
                                    .setOnConfirm {
                                        navigationState.onMarkAsPlayedConfirmed(navigationState.episode)
                                    }
                                    .show(childFragmentManager, "mark_as_played")
                            }
                        }

                        is NavigationState.ShowPodcastEpisodeArchiveConfirmation -> {
                            ConfirmationDialog()
                                .setForceDarkTheme(true)
                                .setSummary(resources.getString(LR.string.player_archive_summary))
                                .setIconId(IR.drawable.ic_archive)
                                .setButtonType(Danger(resources.getString(LR.string.player_archive_title)))
                                .setOnConfirm { navigationState.onArchiveConfirmed(navigationState.episode) }
                                .show(childFragmentManager, "archive")
                        }

                        is NavigationState.ShowUserEpisodeDeleteConfirmation -> {
                            CloudDeleteHelper.getDeleteDialog(navigationState.episode, navigationState.deleteState, navigationState.deleteFunction, resources)
                                .show(childFragmentManager, "archive")
                        }

                        NavigationState.ShowMoreActions -> {
                            // stop double taps
                            if (childFragmentManager.fragments.firstOrNull() is ShelfBottomSheet) return@collect
                            viewModel.episode?.let {
                                ShelfBottomSheet.newInstance(
                                    episodeId = it.uuid,
                                ).show(childFragmentManager, "shelf_bottom_sheet")
                            }
                        }

                        NavigationState.ShowAddBookmark -> {
                            viewModel.buildBookmarkArguments { arguments ->
                                activityLauncher.launch(arguments.getIntent(requireContext()))
                            }
                        }

                        is NavigationState.StartUpsellFlow -> startUpsellFlow(navigationState.source)
                        is NavigationState.ShowReportViolation -> openUrl(navigationState.reportUrl)
                    }
                }
            }
        }
    }

    private fun setupTranscriptPage() {
        binding?.transcriptPage?.setContent {
            TranscriptPageWrapper(
                playerViewModel = viewModel,
                shelfSharedViewModel = shelfSharedViewModel,
                transcriptViewModel = transcriptViewModel,
                searchViewModel = transcriptSearchViewModel,
                theme = theme,
            )
        }
    }

    private fun observeTranscriptPageTransition() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.transitionState.collect { transitionState ->
                    when (transitionState) {
                        is TransitionState.OpenTranscript -> binding?.openTranscript()
                        is TransitionState.CloseTranscript -> binding?.closeTranscript(transitionState.withTransition)
                    }
                }
            }
        }
    }

    private fun AdapterPlayerHeaderBinding.openTranscript() {
        updatePlayerViewsAccessibility(enable = false)
        playerGroup.layoutTransition = LayoutTransition()
        transcriptPage.isVisible = true
        shelf.isVisible = false
        val transcriptShowSeekbarAndPlayerControls = resources.getBoolean(R.bool.transcript_show_seekbar_and_player_controls)
        seekBar.isVisible = transcriptShowSeekbarAndPlayerControls
        playerControls.root.isVisible = transcriptShowSeekbarAndPlayerControls
        playerControls.scale(0.6f)
        if (transcriptShowSeekbarAndPlayerControls) {
            (seekBar.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = resources.getDimensionPixelSize(R.dimen.seekbar_margin_bottom_transcript)
        }
        val containerFragment = parentFragment as? PlayerContainerFragment
        containerFragment?.updateTabsVisibility(false)
        (root as? LockableNestedScrollView)?.setScrollingEnabled(false)
    }

    private fun AdapterPlayerHeaderBinding.closeTranscript(
        withTransition: Boolean,
    ) {
        updatePlayerViewsAccessibility(enable = true)
        playerGroup.layoutTransition = if (withTransition) LayoutTransition() else null
        shelf.isVisible = true
        transcriptPage.isVisible = false
        seekBar.isVisible = true
        playerControls.root.isVisible = true
        playerControls.scale(1f)
        val transcriptShowSeekbarAndPlayerControls = resources.getBoolean(R.bool.transcript_show_seekbar_and_player_controls)
        if (transcriptShowSeekbarAndPlayerControls) {
            (seekBar.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = resources.getDimensionPixelSize(R.dimen.seekbar_margin_bottom)
        }
        val containerFragment = parentFragment as? PlayerContainerFragment
        containerFragment?.updateTabsVisibility(true)
        (root as? LockableNestedScrollView)?.setScrollingEnabled(true)
        playerGroup.layoutTransition = null // Reset to null to avoid animation when changing children visibility anytime later
    }

    private fun AdapterPlayerHeaderBinding.updatePlayerViewsAccessibility(enable: Boolean) {
        val importantForAccessibility = if (enable) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        episodeTitle.importantForAccessibility = importantForAccessibility
        chapterTimeRemaining.importantForAccessibility = importantForAccessibility
        chapterSummary.importantForAccessibility = importantForAccessibility
        nextChapter.importantForAccessibility = importantForAccessibility
        previousChapter.importantForAccessibility = importantForAccessibility
        podcastTitle.importantForAccessibility = importantForAccessibility
    }

    private fun setupUpNextDrag(binding: AdapterPlayerHeaderBinding) {
        val flingGestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (binding.transcriptPage.isVisible) return false
                    val containerFragment = parentFragment as? PlayerContainerFragment ?: return false
                    val upNextBottomSheetBehavior = containerFragment.upNextBottomSheetBehavior

                    return if (velocityY < 0 && abs(velocityY) >= UP_NEXT_FLING_VELOCITY_THRESHOLD && upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        containerFragment.openUpNext()
                        true
                    } else {
                        false
                    }
                }
            },
        )
        @Suppress("ClickableViewAccessibility")
        binding.root.setOnTouchListener { _, event ->
            // This check is a workaround for a behavior between velocityY detected by flingGestureDetector and dragging player bottom sheet.
            // When only the player is expanded and we fling down the velocityY should be positive indicating that direction.
            // However, regardless of flinging up or down the velocityY is always negative because the player's view drags along
            // with a finger and thus velocity computation "gets confused" because MotionEvent positions are relative to the view.
            //
            // Because the fling motion is detected only after we release the finger it means that the player bottom sheet
            // is no longer in an expanded state but in a dragging or a collapsing state.
            if ((activity as? FragmentHostListener)?.getPlayerBottomSheetState() == BottomSheetBehavior.STATE_EXPANDED) {
                flingGestureDetector.onTouchEvent(event)
            } else {
                false
            }
        }
    }

    private fun playerHighlightColor(viewModel: PlayerViewModel.PlayerHeader): Int {
        if (viewModel.isUserEpisode) {
            return theme.getUserFilePlayerHighlightColor()
        }

        return ThemeColor.playerHighlight01(viewModel.theme, viewModel.iconTintColor)
    }

    private var lastLoadedBaseEpisodeId: String? = null
    private var lastUseEpisodeArtwork: Boolean? = null
    private var lastLoadedChapterPath: String? = null

    private fun loadArtwork(
        baseEpisode: BaseEpisode,
        useEpisodeArtwork: Boolean,
        imageView: ImageView,
    ) {
        if (lastLoadedBaseEpisodeId == baseEpisode.uuid && lastUseEpisodeArtwork == useEpisodeArtwork) {
            return
        }

        lastLoadedBaseEpisodeId = baseEpisode.uuid
        lastUseEpisodeArtwork = useEpisodeArtwork
        imageRequestFactory.create(baseEpisode, useEpisodeArtwork).loadInto(imageView)
    }

    private fun loadChapterArtwork(chapter: Chapter?, imageView: ImageView) {
        if (lastLoadedChapterPath == chapter?.imagePath) {
            return
        }

        lastLoadedChapterPath = chapter?.imagePath
        chapter?.imagePath?.let { pathOrUrl ->
            imageRequestFactory.createForFileOrUrl(pathOrUrl).loadInto(imageView)
        } ?: run {
            imageView.setImageDrawable(null)
        }
    }

    override fun onShowNotesClick(episodeUuid: String) {
        val fragment = NotesFragment.newInstance(episodeUuid)
        openBottomSheet(fragment)
    }

    override fun onSkipBack() {
        viewModel.skipBackward()
    }

    override fun onSkipForward() {
        viewModel.skipForward()
    }

    override fun onSkipForwardLongPress() {
        LongPressOptionsFragment().show(parentFragmentManager, "longpressoptions")
    }

    private fun startUpsellFlow(source: OnboardingUpgradeSource) {
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    override fun onPreviousChapter() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_PREVIOUS_CHAPTER_TAPPED)
        viewModel.previousChapter()
    }

    override fun onNextChapter() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_NEXT_CHAPTER_TAPPED)
        viewModel.nextChapter()
    }

    override fun onPlayClicked() {
        if (playbackManager.isPlaying()) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Pause clicked in player")
            playbackManager.pause(sourceView = sourceView)
        } else {
            if (playbackManager.shouldWarnAboutPlayback()) {
                launch {
                    // show the stream warning if the episode isn't downloaded
                    playbackManager.getCurrentEpisode()?.let { episode ->
                        launch(Dispatchers.Main) {
                            if (episode.isDownloaded) {
                                viewModel.play()
                                warningsHelper.showBatteryWarningSnackbarIfAppropriate(snackbarParentView = view)
                            } else {
                                warningsHelper.streamingWarningDialog(episode = episode, snackbarParentView = view, sourceView = sourceView)
                                    .show(parentFragmentManager, "streaming dialog")
                            }
                        }
                    }
                }
            } else {
                viewModel.play()
                warningsHelper.showBatteryWarningSnackbarIfAppropriate(snackbarParentView = view)
            }
        }
    }

    override fun onClosePlayer() {
        (activity as FragmentHostListener).closePlayer()
    }

    override fun onPictureInPictureClick() {
        val context = context ?: return
        context.startActivity(VideoActivity.buildIntent(enterPictureInPicture = true, context = context))
    }

    override fun onFullScreenVideoClick() {
        val context = context ?: return
        context.startActivity(VideoActivity.buildIntent(context = context))
    }

    private fun openBottomSheet(fragment: Fragment) {
        (activity as FragmentHostListener).showBottomSheet(fragment)
    }

    override fun onSeekPositionChangeStop(progress: Int, seekComplete: () -> Unit) {
        viewModel.seekToMs(progress, seekComplete)
    }

    override fun onHeaderChapterClick(chapter: Chapter) {
        (parentFragment as? PlayerContainerFragment)?.openChaptersAt(chapter)
    }

    private fun showViewBookmarksSnackbar(result: BookmarkActivityContract.BookmarkResult?) {
        val view = view
        if (result == null || view == null) {
            return
        }

        val snackbarMessage = if (result.isExistingBookmark) {
            getString(LR.string.bookmark_updated, result.title)
        } else {
            getString(LR.string.bookmark_added, result.title)
        }
        val viewBookmarksAction = View.OnClickListener {
            (parentFragment as? PlayerContainerFragment)?.openBookmarks()
        }

        Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_LONG)
            .setAction(LR.string.settings_view, viewBookmarksAction)
            .setActionTextColor(result.tintColor)
            .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.DARK))
            .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.DARK))
            .show()
    }

    private fun showSnackBar(text: CharSequence) {
        parentFragment?.view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.LIGHT))
                .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.LIGHT))
                .show()
        }
    }
}
