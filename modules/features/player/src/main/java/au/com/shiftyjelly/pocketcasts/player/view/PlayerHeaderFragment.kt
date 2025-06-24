package au.com.shiftyjelly.pocketcasts.player.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.ads.AdReportFragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.ad.AdBanner
import au.com.shiftyjelly.pocketcasts.compose.ad.BlazeAd
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterPlayerHeaderBinding
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivity
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkImageState
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkOrVideo
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkOrVideoState
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.EpisodeTitles
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerControls
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.player.view.shelf.PlayerShelf
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.SnackbarMessage
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.transcripts.ui.TranscriptPage
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType.Danger
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.transcripts.UiState as TranscriptsUiState
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val UP_NEXT_FLING_VELOCITY_THRESHOLD = 1000.0f

@AndroidEntryPoint
class PlayerHeaderFragment : BaseFragment(), PlayerClickListener {
    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var warningsHelper: WarningsHelper

    private val viewModel: PlayerViewModel by activityViewModels()
    private val shelfSharedViewModel: ShelfSharedViewModel by activityViewModels()
    private val transcriptViewModel by viewModels<TranscriptViewModel>(
        ownerProducer = { requireParentFragment() },
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<TranscriptViewModel.Factory> { factory ->
                factory.create(TranscriptViewModel.Source.Player)
            }
        },
    )
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

        requireNotNull(binding).composeContent.setContentWithViewCompositionStrategy {
            val podcastColors by remember { podcastColorsFlow() }.collectAsState(PodcastColors.ForUserEpisode)

            val headerData by remember { playerHeaderFlow() }.collectAsState(PlayerViewModel.PlayerHeader())
            val artworkOrVideoState by remember { playerVisualsStateFlow() }.collectAsState(ArtworkOrVideoState.NoContent)
            val ads by viewModel.activeAds.collectAsState()

            val isTranscriptOpen by shelfSharedViewModel.isTranscriptOpen.collectAsState()
            val transcriptUiState by transcriptViewModel.uiState.collectAsState()

            val transitionData = updateTranscriptTransitionData(
                isTranscriptOpen = isTranscriptOpen,
                showTranscriptPaywall = transcriptUiState.isPaywallVisible,
            )

            AppTheme(theme.activeTheme) {
                CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
                    val playerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .background(playerColors.background01)
                            .fillMaxSize(),
                    ) {
                        TranscriptContent(
                            state = transcriptUiState,
                            isVisible = transitionData.isTranscriptOpen,
                            modifier = Modifier
                                .fillMaxWidth(fraction = ResourcesCompat.getFloat(resources, UR.dimen.player_max_content_width_fraction))
                                .fillMaxHeight()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        )
                        VerticalPlayerContent(
                            ad = ads.firstOrNull(),
                            artworkOrVideoState = artworkOrVideoState,
                            headerData = headerData,
                            playerColors = playerColors,
                            transitionData = transitionData,
                            modifier = Modifier
                                .fillMaxWidth(fraction = ResourcesCompat.getFloat(resources, UR.dimen.player_max_content_width_fraction))
                                .fillMaxHeight()
                                .padding(16.dp),
                        )
                    }
                }
            }

            LoadTranscriptEffect(
                isTranscriptOpen = transitionData.isTranscriptOpen,
                playerEpisodeUuid = headerData.episodeUuid,
                transcriptEpisodeUuid = transcriptUiState.transcriptEpisodeUuid,
            )
        }

        observeNavigationState()
        observeShelfItemNavigationState()
        observeTranscriptPageTransition()
        observeSnackbarMessages()
        setupUpNextDrag()
    }

    private fun observeNavigationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { navigationState ->
                    when (navigationState) {
                        is PlayerViewModel.NavigationState.ShowStreamingWarningDialog -> {
                            warningsHelper.streamingWarningDialog(episode = navigationState.episode, snackbarParentView = view, sourceView = sourceView)
                                .show(parentFragmentManager, "streaming dialog")
                        }

                        PlayerViewModel.NavigationState.ShowSkipForwardLongPressOptionsDialog -> {
                            LongPressOptionsFragment().show(parentFragmentManager, "longpressoptions")
                        }

                        is PlayerViewModel.NavigationState.OpenChapterAt -> {
                            (parentFragment as? PlayerContainerFragment)?.openChaptersAt(navigationState.chapter)
                        }

                        is PlayerViewModel.NavigationState.OpenPodcastPage -> {
                            (activity as? FragmentHostListener)?.let { listener ->
                                listener.closePlayer()
                                listener.openPodcastPage(navigationState.podcastUuid, navigationState.source.analyticsValue)
                            }
                        }

                        is PlayerViewModel.NavigationState.OpenChapterUrl -> {
                            val chapterUrl = navigationState.chapterUrl
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = chapterUrl.toUri()
                            try {
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Timber.e(e)
                                UiUtil.displayAlertError(requireContext(), getString(LR.string.player_open_url_failed, chapterUrl), null)
                            }
                        }
                    }
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
                            val bookmarkArguments = viewModel.createBookmarkArguments()
                            if (bookmarkArguments != null) {
                                activityLauncher.launch(BookmarkActivity.launchIntent(requireContext(), bookmarkArguments))
                            }
                        }

                        is NavigationState.StartUpsellFlow -> startUpsellFlow(navigationState.source)
                    }
                }
            }
        }
    }

    private fun observeTranscriptPageTransition() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasTranscriptOpen = shelfSharedViewModel.isTranscriptOpen.value

                shelfSharedViewModel.isTranscriptOpen.collect { isTranscriptOpen ->
                    val uiState = transcriptViewModel.uiState.value

                    if (!wasTranscriptOpen && isTranscriptOpen) {
                        val containerFragment = parentFragment as? PlayerContainerFragment
                        containerFragment?.updateTabsVisibility(false)
                        binding?.root?.setScrollingEnabled(false)
                    } else if (wasTranscriptOpen && !isTranscriptOpen) {
                        val event = if (uiState.isPaywallVisible) {
                            AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_DISMISSED
                        } else {
                            AnalyticsEvent.TRANSCRIPT_DISMISSED
                        }
                        transcriptViewModel.track(event)
                        val containerFragment = parentFragment as? PlayerContainerFragment
                        containerFragment?.updateTabsVisibility(true)
                        binding?.root?.setScrollingEnabled(true)
                    }
                    wasTranscriptOpen = isTranscriptOpen
                }
            }
        }
    }

    private fun observeSnackbarMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarMessages.collect { message ->
                    if (message == PlayerViewModel.SnackbarMessage.ShowBatteryWarningIfAppropriate) {
                        warningsHelper.showBatteryWarningSnackbarIfAppropriate(snackbarParentView = view)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shelfSharedViewModel.snackbarMessages.collect { message ->
                    val text = when (message) {
                        SnackbarMessage.EpisodeDownloadStarted -> LR.string.episode_queued_for_download
                        SnackbarMessage.EpisodeRemoved -> LR.string.episode_was_removed
                        SnackbarMessage.TranscriptNotAvailable -> LR.string.transcript_error_not_available
                        SnackbarMessage.ShareNotAvailable -> LR.string.sharing_is_not_available_for_private_podcasts
                    }
                    showSnackBar(text = getString(text))
                }
            }
        }
    }

    private fun setupUpNextDrag() {
        val flingGestureDetector = GestureDetector(
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
        binding?.root?.setOnTouchListener { _, event ->
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

    override fun onShowNotesClick(episodeUuid: String) {
        val fragment = NotesFragment.newInstance(episodeUuid)
        openBottomSheet(fragment)
    }

    private fun startUpsellFlow(source: OnboardingUpgradeSource) {
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
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

    private fun openAd(ad: BlazeAd) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, ad.ctaUrl.toUri())
            startActivity(intent)
        }.onFailure { LogBuffer.e("Ads", it, "Failed to open an ad: ${ad.id}") }
    }

    private fun openAdReportSheet(ad: BlazeAd, podcastColors: PodcastColors) {
        if (parentFragmentManager.findFragmentByTag("ad_report") == null) {
            AdReportFragment
                .newInstance(ad, podcastColors)
                .show(parentFragmentManager, "ad_report")
        }
    }

    private fun playerHeaderFlow(): Flow<PlayerViewModel.PlayerHeader> {
        return viewModel.listDataRx.map { it.podcastHeader }.asFlow()
    }

    private fun podcastColorsFlow(): Flow<PodcastColors> {
        return viewModel.podcastFlow.map { podcast ->
            podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode
        }
    }

    private fun playerVisualsStateFlow(): Flow<ArtworkOrVideoState> {
        val listDataFlow = viewModel.listDataLive
            .asFlow()
            .distinctUntilChanged(::isListDataEquivalentForVisuals)
        return combine(listDataFlow, viewModel.playerFlow, ::createPlayerVisualState)
    }

    private fun isListDataEquivalentForVisuals(old: PlayerViewModel.ListData, new: PlayerViewModel.ListData): Boolean {
        return old.podcastHeader.episode?.uuid == new.podcastHeader.episode?.uuid &&
            old.podcastHeader.useEpisodeArtwork == new.podcastHeader.useEpisodeArtwork &&
            old.podcastHeader.chapter?.index == new.podcastHeader.chapter?.index &&
            old.podcastHeader.isPrepared == new.podcastHeader.isPrepared
    }

    private fun createPlayerVisualState(
        listData: PlayerViewModel.ListData,
        player: Player?,
    ): ArtworkOrVideoState {
        val header = listData.podcastHeader
        return when {
            header.isVideo -> ArtworkOrVideoState.Video(
                player = player,
                chapterUrl = header.chapter?.url,
            )

            header.episode != null -> {
                val chapterPath = header.chapter?.imagePath
                ArtworkOrVideoState.Artwork(
                    artworkImageState = when {
                        chapterPath != null -> ArtworkImageState.Chapter(chapterPath)
                        header.useEpisodeArtwork -> ArtworkImageState.Episode(header.episode)
                        else -> ArtworkImageState.Podcast(header.episode)
                    },
                    chapterUrl = header.chapter?.url,
                )
            }

            else -> ArtworkOrVideoState.NoContent
        }
    }

    @Composable
    private fun VerticalPlayerContent(
        ad: BlazeAd?,
        artworkOrVideoState: ArtworkOrVideoState,
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        transitionData: TranscriptTransitionData,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(transitionData.nonTranscriptElementsAlpha),
            ) {
                if (!transitionData.isTranscriptOpen) {
                    AnimatedNonNullVisibility(
                        item = ad,
                        enter = adEnterTransition,
                        exit = adExitTransition,
                    ) { ad ->
                        AdBanner(
                            ad = ad,
                            colors = rememberAdColors().bannerAd,
                            onAdClick = { openAd(ad) },
                            onOptionsClick = { openAdReportSheet(ad, playerColors.podcastColors) },
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    ArtworkOrVideo(
                        state = artworkOrVideoState,
                        onChapterUrlClick = viewModel::onChapterUrlClick,
                        configureVideoView = { videoView ->
                            videoView.setOnClickListener { onFullScreenVideoClick() }
                        },
                    )
                }
            }
            if (!transitionData.isTranscriptOpen) {
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                EpisodeTitles(
                    playerColors = playerColors,
                    playerViewModel = viewModel,
                    modifier = Modifier.alpha(transitionData.nonTranscriptElementsAlpha),
                )
            }
            AnimatedVisibility(
                visible = transitionData.showPlayerControls,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                    PlayerSeekBar(
                        playbackPosition = headerData.positionMs.milliseconds,
                        playbackDuration = headerData.durationMs.milliseconds,
                        adjustPlaybackDuration = headerData.adjustRemainingTimeDuration,
                        playbackSpeed = headerData.playbackEffects.playbackSpeed,
                        chapters = headerData.chapters,
                        isBuffering = headerData.isBuffering,
                        bufferedUpTo = headerData.bufferedUpToMs.milliseconds,
                        playerColors = playerColors,
                        onSeekToPosition = { progress, onSeekComplete ->
                            val progressMs = progress.inWholeMilliseconds.toInt()
                            viewModel.seekToMs(progressMs, onSeekComplete)
                            playbackManager.trackPlaybackSeek(progressMs, SourceView.PLAYER)
                        },
                        modifier = Modifier.offset { transitionData.seekBarOffset },
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    PlayerControls(
                        playerColors = playerColors,
                        playerViewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .scale(transitionData.controlsScale)
                            .offset { transitionData.controlsOffset },
                    )
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.large_play_button_margin_bottom)),
                    )
                }
            }
            AnimatedVisibility(
                visible = !transitionData.isTranscriptOpen,
                enter = shelfEnterTransition,
                exit = shelfExitTransition,
            ) {
                PlayerShelf(
                    playerColors = playerColors,
                    shelfSharedViewModel = shelfSharedViewModel,
                    playerViewModel = viewModel,
                )
            }
        }
    }

    @Composable
    private fun TranscriptContent(
        state: TranscriptsUiState,
        isVisible: Boolean,
        modifier: Modifier = Modifier,
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = transcriptEnterTransition,
            exit = transcriptExitTransition,
            modifier = modifier,
        ) {
            TranscriptPage(
                uiState = state,
                toolbarPadding = PaddingValues(horizontal = 16.dp),
                paywallPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                transcriptPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                onClickClose = {
                    transcriptViewModel.hideSearch()
                    shelfSharedViewModel.closeTranscript()
                },
                onClickReload = transcriptViewModel::reloadTranscript,
                onUpdateSearchTerm = transcriptViewModel::searchInTranscript,
                onClearSearchTerm = transcriptViewModel::clearSearch,
                onSelectPreviousSearch = transcriptViewModel::selectPreviousSearchMatch,
                onSelectNextSearch = transcriptViewModel::selectNextSearchMatch,
                onShowSearchBar = transcriptViewModel::openSearch,
                onHideSearchBar = transcriptViewModel::hideSearch,
                onClickSubscribe = {
                    transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SUBSCRIBE_TAPPED)
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.GENERATED_TRANSCRIPTS))
                },
                onShowTranscript = { transcript ->
                    val properties = mapOf(
                        "type" to transcript.type.analyticsValue,
                        "show_as_webpage" to (transcript is Transcript.Web),
                    )
                    transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_SHOWN, properties)
                },
                onShowTransciptPaywall = {
                    transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN)
                },
            )
        }
    }

    @Composable
    private fun updateTranscriptTransitionData(
        isTranscriptOpen: Boolean,
        showTranscriptPaywall: Boolean,
    ): TranscriptTransitionData {
        val controlsOffsetValue = LocalDensity.current.run { 64.dp.roundToPx() }

        val transcriptOpenState = rememberUpdatedState(isTranscriptOpen)
        val transition = updateTransition(transcriptOpenState.value)
        val nonTranscriptElementsAlpha = transition.animateFloat { showTranscript ->
            if (showTranscript) 0f else 1f
        }
        val controlsScale = transition.animateFloat { showTranscript ->
            if (showTranscript) 0.6f else 1f
        }
        val seekbarOffset = transition.animateIntOffset { showTranscript ->
            if (showTranscript) IntOffset(x = 0, y = controlsOffsetValue) else IntOffset.Zero
        }
        val showPlayerControls = rememberUpdatedState(if (transcriptOpenState.value) !showTranscriptPaywall else true)

        return remember(transition, transcriptOpenState, showPlayerControls) {
            TranscriptTransitionData(
                isTranscriptOpen = transcriptOpenState,
                nonTranscriptElementsAlpha = nonTranscriptElementsAlpha,
                showPlayerControls = showPlayerControls,
                seekbarOffset = seekbarOffset,
                controlsScale = controlsScale,
            )
        }
    }

    @Composable
    private fun LoadTranscriptEffect(
        isTranscriptOpen: Boolean,
        playerEpisodeUuid: String,
        transcriptEpisodeUuid: String?,
    ) {
        LaunchedEffect(isTranscriptOpen, playerEpisodeUuid, transcriptEpisodeUuid) {
            if (isTranscriptOpen && playerEpisodeUuid != transcriptEpisodeUuid) {
                transcriptViewModel.loadTranscript(playerEpisodeUuid)
            }
        }
    }
}

private class TranscriptTransitionData(
    isTranscriptOpen: State<Boolean>,
    nonTranscriptElementsAlpha: State<Float>,
    showPlayerControls: State<Boolean>,
    seekbarOffset: State<IntOffset>,
    controlsScale: State<Float>,
) {
    val isTranscriptOpen by isTranscriptOpen
    val nonTranscriptElementsAlpha by nonTranscriptElementsAlpha
    val seekBarOffset by seekbarOffset
    val showPlayerControls by showPlayerControls
    val controlsOffset get() = seekBarOffset * 0.9f
    val controlsScale by controlsScale
}

private val adEnterTransition = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow))
private val adExitTransition = fadeOut(spring(stiffness = Spring.StiffnessVeryLow)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))

private val transcriptEnterTransition = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)) + slideInVertically(initialOffsetY = { it })
private val transcriptExitTransition = fadeOut(spring(stiffness = Spring.StiffnessHigh))

private val shelfEnterTransition = fadeIn() + expandVertically(expandFrom = Alignment.Top)
private val shelfExitTransition = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
