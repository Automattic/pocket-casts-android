package au.com.shiftyjelly.pocketcasts.player.view

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
import androidx.core.view.GestureDetectorCompat
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
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.binding.playIfTrue
import au.com.shiftyjelly.pocketcasts.player.binding.setSeekBarState
import au.com.shiftyjelly.pocketcasts.player.binding.showIfPresent
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterPlayerHeaderBinding
import au.com.shiftyjelly.pocketcasts.player.view.ShelfFragment.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
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
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import au.com.shiftyjelly.pocketcasts.views.helper.toCircle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val UP_NEXT_FLING_VELOCITY_THRESHOLD = 1000.0f

@AndroidEntryPoint
class PlayerHeaderFragment : BaseFragment(), PlayerClickListener {
    @Inject lateinit var castManager: CastManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var warningsHelper: WarningsHelper

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var bookmarkFeature: BookmarkFeatureControl

    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory
    private val viewModel: PlayerViewModel by activityViewModels()
    private val transcriptViewModel by viewModels<TranscriptViewModel>({ requireParentFragment() })
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

        binding.skipBack.setOnClickListener {
            onSkipBack()
            (it as LottieAnimationView).playAnimation()
        }
        binding.skipForward.setOnClickListener {
            onSkipForward()
            (it as LottieAnimationView).playAnimation()
        }
        binding.skipForward.setOnLongClickListener {
            onSkipForwardLongPress()
            (it as LottieAnimationView).playAnimation()
            true
        }
        binding.largePlayButton.setOnPlayClicked {
            onPlayClicked()
        }
        binding.seekBar.changeListener = object : PlayerSeekBar.OnUserSeekListener {
            override fun onSeekPositionChangeStop(progress: Int, seekComplete: () -> Unit) {
                viewModel.seekToMs(progress, seekComplete)
                playbackManager.trackPlaybackSeek(progress, SourceView.PLAYER)
            }

            override fun onSeekPositionChanging(progress: Int) {}

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
            ShelfItem.Report to binding.report,
        )
        viewModel.trimmedShelfLive.observe(viewLifecycleOwner) {
            binding.shelf.removeAllViews()
            it.first.subList(0, 4)
                .mapNotNull(shelfViews::get)
                .forEach { itemView -> binding.shelf += itemView }
            binding.shelf.addView(binding.playerActions)
        }

        if (FeatureFlag.isEnabled(Feature.TRANSCRIPTS)) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    transcriptViewModel.uiState.collect { uiState ->
                        val transcriptAvailable = uiState !is TranscriptViewModel.UiState.Empty
                        binding.transcript?.isEnabled = transcriptAvailable
                        binding.transcript?.alpha = if (transcriptAvailable) 1f else 0.5f
                    }
                }
            }
        }

        binding.effects.setOnClickListener { onEffectsClick() }
        binding.previousChapter.setOnClickListener { onPreviousChapter() }
        binding.nextChapter.setOnClickListener { onNextChapter() }
        binding.sleep.setOnClickListener { onSleepClick() }
        binding.star.setOnClickListener { onStarClick() }
        binding.share.setOnClickListener { onShareClick() }
        binding.playerActions.setOnClickListener { onMoreClicked() }
        binding.podcast.setOnClickListener { showPodcast() }
        binding.played.setOnClickListener {
            trackShelfAction(ShelfItem.Played.analyticsValue)
            viewModel.markCurrentlyPlayingAsPlayed(requireContext())?.show(childFragmentManager, "mark_as_played")
        }
        binding.archive.setOnClickListener {
            trackShelfAction(ShelfItem.Archive.analyticsValue)
            viewModel.archiveCurrentlyPlaying(resources)?.show(childFragmentManager, "archive")
        }
        binding.bookmark.setOnClickListener {
            trackShelfAction(ShelfItem.Bookmark.analyticsValue)
            onAddBookmarkClick(OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION)
        }
        binding.transcript?.setOnClickListener {
            Timber.d("Transcript clicked from bottom shelf")
        }
        binding.report?.setOnClickListener {
            trackShelfAction(ShelfItem.Report.analyticsValue)
            openUrl(settings.getReportViolationUrl())
        }
        binding.videoView.playbackManager = playbackManager
        binding.videoView.setOnClickListener { onFullScreenVideoClick() }

        with(binding.castButton) {
            CastButtonFactory.setUpMediaRouteButton(binding.root.context, this)
            visibility = View.VISIBLE
            updateColor(ThemeColor.playerContrast03(theme.activeTheme))
            setOnClickListener {
                trackShelfAction(ShelfItem.Cast.analyticsValue)
                chromeCastAnalytics.trackChromeCastViewShown()
            }
        }

        setupUpNextDrag(binding)

        viewModel.listDataLive.observe(viewLifecycleOwner) {
            val headerViewModel = it.podcastHeader

            binding.largePlayButton.setPlaying(isPlaying = headerViewModel.isPlaying, animate = true)

            val playerContrast1 = ThemeColor.playerContrast01(headerViewModel.theme)
            binding.largePlayButton.setCircleTintColor(playerContrast1)
            binding.skipBackText.setTextColor(playerContrast1)
            binding.jumpForwardText.setTextColor(playerContrast1)
            binding.skipBack.post { // this only works the second time it's called unless it's in a post
                binding.skipBack.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { SimpleColorFilter(playerContrast1) }
                binding.skipForward.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { SimpleColorFilter(playerContrast1) }
            }
            binding.episodeTitle.setTextColor(playerContrast1)

            headerViewModel.episode?.let { episode ->
                loadArtwork(episode, headerViewModel.useEpisodeArtwork, binding.artwork)
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
                durationMs = headerViewModel.durationMs,
                positionMs = headerViewModel.positionMs,
                tintColor = headerViewModel.iconTintColor,
                bufferedUpTo = headerViewModel.bufferedUpToMs,
                isBuffering = headerViewModel.isBuffering,
                theme = headerViewModel.theme,
            )
            binding.skipForward.toCircle(true)
            binding.jumpForwardText.text = headerViewModel.skipForwardInSecs.toString()
            binding.skipBack.toCircle(true)
            binding.skipBackText.text = headerViewModel.skipBackwardInSecs.toString()
            binding.sleep.playIfTrue(headerViewModel.isSleepRunning)
        }
    }

    private fun setupUpNextDrag(binding: AdapterPlayerHeaderBinding) {
        val flingGestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
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

    override fun onEffectsClick() {
        trackShelfAction(ShelfItem.Effects.analyticsValue)
        EffectsFragment().show(parentFragmentManager, "effects_sheet")
    }

    override fun onSleepClick() {
        trackShelfAction(ShelfItem.Sleep.analyticsValue)
        SleepFragment().show(parentFragmentManager, "sleep_sheet")
    }

    override fun onStarClick() {
        trackShelfAction(ShelfItem.Star.analyticsValue)
        viewModel.starToggle()
    }

    fun onAddBookmarkClick(source: OnboardingUpgradeSource) {
        if (bookmarkFeature.isAvailable(settings.userTier)) {
            viewModel.buildBookmarkArguments { arguments ->
                activityLauncher.launch(arguments.getIntent(requireContext()))
            }
        } else {
            startUpsellFlow(source)
        }
    }

    private fun startUpsellFlow(source: OnboardingUpgradeSource) {
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    override fun onShareClick() {
        trackShelfAction(ShelfItem.Share.analyticsValue)
        ShareFragment.newInstance(SourceView.PLAYER).show(parentFragmentManager, "share_sheet")
    }

    private fun showPodcast() {
        trackShelfAction(ShelfItem.Podcast.analyticsValue)
        val podcast = viewModel.podcast
        (activity as FragmentHostListener).closePlayer()
        if (podcast != null) {
            (activity as? FragmentHostListener)?.openPodcastPage(podcast.uuid, sourceView.analyticsValue)
        } else {
            (activity as? FragmentHostListener)?.openCloudFiles()
        }
    }

    override fun onPreviousChapter() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_PREVIOUS_CHAPTER_TAPPED)
        viewModel.previousChapter()
    }

    override fun onNextChapter() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_NEXT_CHAPTER_TAPPED)
        viewModel.nextChapter()
    }

    fun onMoreClicked(sourceView: SourceView? = null) {
        // stop double taps
        if (childFragmentManager.fragments.firstOrNull() is ShelfBottomSheet) {
            return
        }
        analyticsTracker.track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_SHOWN)
        ShelfBottomSheet.newInstance(sourceView).show(childFragmentManager, "shelf_bottom_sheet")
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

    private fun trackShelfAction(analyticsAction: String) {
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_ACTION_TAPPED,
            mapOf(AnalyticsProp.Key.FROM to AnalyticsProp.Value.SHELF, AnalyticsProp.Key.ACTION to analyticsAction),
        )
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
}
