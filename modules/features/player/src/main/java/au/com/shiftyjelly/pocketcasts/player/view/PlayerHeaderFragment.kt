package au.com.shiftyjelly.pocketcasts.player.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtLeastMediumHeight
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtLeastMediumWidth
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.components.rememberNestedScrollLockableInteropConnection
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivity
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkImage
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkImageState
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkOrVideo
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.ArtworkOrVideoState
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.EpisodeTitles
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerControls
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.player.view.nowplaying.VideoBox
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
import au.com.shiftyjelly.pocketcasts.transcripts.ui.TranscriptShareButton
import au.com.shiftyjelly.pocketcasts.ui.extensions.inPortrait
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType.Danger
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.transcripts.UiState as TranscriptsUiState

@AndroidEntryPoint
class PlayerHeaderFragment :
    BaseFragment(),
    PlayerClickListener {
    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var warningsHelper: WarningsHelper

    @Inject
    lateinit var addToPlaylistFragmentFactory: AddToPlaylistFragmentFactory

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
    private val sourceView = SourceView.PLAYER

    private val activityLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(BookmarkActivityContract()) { result ->
        showViewBookmarksSnackbar(result)
    }

    private val showUpNextFlingBehavior = object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            if (isPlayerExpanded() && isUpNextCollapsed() && initialVelocity > 2000f) {
                (parentFragment as PlayerContainerFragment).openUpNext()
            }
            return 0f
        }

        private fun isPlayerExpanded(): Boolean {
            val playerSheetState = (requireActivity() as FragmentHostListener).getPlayerBottomSheetState()
            return playerSheetState == BottomSheetBehavior.STATE_EXPANDED
        }

        private fun isUpNextCollapsed(): Boolean {
            val upNextSheetState = (requireParentFragment() as PlayerContainerFragment).upNextBottomSheetBehavior.state
            return upNextSheetState in listOf(BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val windowSize = currentWindowAdaptiveInfo().windowSizeClass
        val isPortraitConfiguration = LocalConfiguration.current.inPortrait()
        val isPortraitPlayer = isPortraitConfiguration || windowSize.isAtLeastMediumHeight()
        val maxWidthFraction = if (isPortraitConfiguration && windowSize.isAtLeastMediumWidth()) 0.8f else 1f

        val podcastColors by remember { podcastColorsFlow() }.collectAsState(PodcastColors.ForUserEpisode)
        val headerData by remember { playerHeaderFlow() }.collectAsState(PlayerViewModel.PlayerHeader())
        val artworkOrVideoState by remember { playerVisualsStateFlow() }.collectAsState(ArtworkOrVideoState.NoContent)
        val activeAd by viewModel.activeAd.collectAsState()

        val isPlayerOpen by isPlayerOpenFlow().collectAsState(false)
        val isTranscriptOpen by shelfSharedViewModel.isTranscriptOpen.collectAsState()
        val transcriptUiState by transcriptViewModel.uiState.collectAsState()

        val transitionData = updateTranscriptTransitionData(
            isTranscriptOpen = isTranscriptOpen,
            isTranscriptPaywallOpen = transcriptUiState.isPaywallVisible,
        )

        AppTheme(theme.activeTheme) {
            CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
                val playerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .background(playerColors.background01)
                        .fillMaxSize()
                        .nestedScroll(rememberNestedScrollLockableInteropConnection().apply { isEnabled = !isTranscriptOpen }),
                ) {
                    TranscriptContent(
                        state = transcriptUiState,
                        isVisible = transitionData.isTranscriptOpen,
                        isPortraitPlayer = isPortraitPlayer,
                        modifier = Modifier
                            .fillMaxWidth(fraction = maxWidthFraction)
                            .fillMaxHeight()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    )
                    PlayerContent(
                        ad = activeAd,
                        artworkOrVideoState = artworkOrVideoState,
                        headerData = headerData,
                        playerColors = playerColors,
                        transitionData = transitionData,
                        isPortraitPlayer = isPortraitPlayer,
                        modifier = Modifier
                            .fillMaxWidth(fraction = maxWidthFraction)
                            .fillMaxHeight()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    )
                }
            }
        }

        LoadTranscriptEffect(
            isTranscriptOpen = transitionData.isTranscriptOpen,
            playerEpisodeUuid = headerData.episodeUuid,
            transcriptEpisodeUuid = transcriptUiState.transcriptEpisodeUuid,
        )

        AdImpressionEffect(activeAd, isPlayerOpen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNavigationState()
        observeShelfItemNavigationState()
        observeTranscriptPageTransition()
        observeSnackbarMessages()
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

                        is NavigationState.AddEpisodeToPlaylist -> {
                            if (parentFragmentManager.findFragmentByTag("add-to-playlist") == null) {
                                val fragment = addToPlaylistFragmentFactory.create(
                                    episodeUuid = navigationState.episodeUuid,
                                    customTheme = Theme.ThemeType.DARK,
                                )
                                fragment.show(parentFragmentManager, "add-to-playlist")
                            }
                        }
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
                    } else if (wasTranscriptOpen && !isTranscriptOpen) {
                        val event = if (uiState.isPaywallVisible) {
                            AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_DISMISSED
                        } else {
                            AnalyticsEvent.TRANSCRIPT_DISMISSED
                        }
                        transcriptViewModel.track(event)
                        val containerFragment = parentFragment as? PlayerContainerFragment
                        containerFragment?.updateTabsVisibility(true)
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
        viewModel.trackAdTapped(ad)
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, ad.url.toUri())
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

    private fun isPlayerOpenFlow() = callbackFlow<Boolean> {
        val callback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                trySendBlocking(newState == BottomSheetBehavior.STATE_EXPANDED)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }
        val hostListener = (requireActivity() as FragmentHostListener)
        hostListener.addPlayerBottomSheetCallback(callback)
        awaitClose {
            hostListener.removePlayerBottomSheetCallback(callback)
        }
    }

    @Composable
    private fun PlayerContent(
        ad: BlazeAd?,
        artworkOrVideoState: ArtworkOrVideoState,
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        transitionData: TranscriptTransitionData,
        isPortraitPlayer: Boolean,
        modifier: Modifier = Modifier,
    ) {
        if (isPortraitPlayer) {
            VerticalPlayerContent(
                ad = ad,
                artworkOrVideoState = artworkOrVideoState,
                headerData = headerData,
                playerColors = playerColors,
                transitionData = transitionData,
                modifier = modifier.navigationBarsPadding(),
            )
        } else {
            HorizontalPlayerContent(
                ad = ad,
                artworkOrVideoState = artworkOrVideoState,
                headerData = headerData,
                playerColors = playerColors,
                transitionData = transitionData,
                modifier = modifier,
            )
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
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                // Do not apply scroll modifiers when transcripts are open so toolbar click events are not consumed.
                .then(
                    if (transitionData.isTranscriptOpen) {
                        Modifier
                    } else {
                        Modifier.verticalScroll(scrollState, flingBehavior = showUpNextFlingBehavior)
                    },
                ),
        ) {
            AdAndArtwork(
                ad = ad,
                artworkOrVideoState = artworkOrVideoState,
                isTranscriptOpen = transitionData.isTranscriptOpen,
                playerColors = playerColors,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(transitionData.nonTranscriptElementsAlpha),
            )
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
                        headerData = headerData,
                        playerColors = playerColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { transitionData.seekBarOffset },
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
    private fun HorizontalPlayerContent(
        ad: BlazeAd?,
        artworkOrVideoState: ArtworkOrVideoState,
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        transitionData: TranscriptTransitionData,
        modifier: Modifier = Modifier,
    ) {
        AnimatedVisibility(
            visible = !transitionData.isTranscriptOpen,
            enter = shelfEnterTransition,
            exit = shelfExitTransition,
            modifier = modifier,
        ) {
            if (ad == null) {
                HorizontalPlayerContentWithoutAd(
                    artworkOrVideoState = artworkOrVideoState,
                    headerData = headerData,
                    playerColors = playerColors,
                )
            } else {
                HorizontalPlayerContentWithAd(
                    ad = ad,
                    artworkOrVideoState = artworkOrVideoState,
                    headerData = headerData,
                    playerColors = playerColors,
                )
            }
        }
    }

    @Composable
    private fun HorizontalPlayerContentWithAd(
        ad: BlazeAd,
        artworkOrVideoState: ArtworkOrVideoState,
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        modifier: Modifier = Modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState(), flingBehavior = showUpNextFlingBehavior),
            ) {
                AdAndArtworkHorizontal(
                    artworkOrVideoState = artworkOrVideoState,
                    playerColors = playerColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                PlayerSeekBar(
                    headerData = headerData,
                    playerColors = playerColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
                PlayerControls(
                    playerColors = playerColors,
                    playerViewModel = viewModel,
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                PlayerShelf(
                    playerColors = playerColors,
                    shelfSharedViewModel = shelfSharedViewModel,
                    playerViewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState(), flingBehavior = showUpNextFlingBehavior),
            ) {
                Spacer(
                    modifier = Modifier.weight(1f),
                )
                AdBanner(
                    ad = ad,
                    colors = rememberAdColors().bannerAd,
                    onAdClick = { openAd(ad) },
                    onOptionsClick = { openAdReportSheet(ad, playerColors.podcastColors) },
                )
                Spacer(
                    modifier = Modifier.weight(3f),
                )
            }
        }
    }

    @Composable
    private fun HorizontalPlayerContentWithoutAd(
        artworkOrVideoState: ArtworkOrVideoState,
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), flingBehavior = showUpNextFlingBehavior),
        ) {
            Row {
                val windowWithPx = LocalWindowInfo.current.containerSize.width
                val windowWidthDp = LocalDensity.current.run { windowWithPx.toDp() }
                val maxSize = when (artworkOrVideoState) {
                    is ArtworkOrVideoState.Video -> if (windowWidthDp >= 720.dp) 360.dp else 280.dp
                    is ArtworkOrVideoState.Artwork, is ArtworkOrVideoState.NoContent -> 200.dp
                }
                ArtworkOrVideo(
                    state = artworkOrVideoState,
                    onChapterUrlClick = {},
                    configureVideoView = { videoView ->
                        videoView.setOnClickListener { onFullScreenVideoClick() }
                    },
                    modifier = Modifier.sizeIn(maxWidth = maxSize, maxHeight = maxSize),
                )
                Spacer(
                    modifier = Modifier.width(32.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EpisodeTitles(
                        playerColors = playerColors,
                        playerViewModel = viewModel,
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    PlayerSeekBar(
                        headerData = headerData,
                        playerColors = playerColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier.height(12.dp),
                    )
                    PlayerControls(
                        playerColors = playerColors,
                        playerViewModel = viewModel,
                    )
                }
            }
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            PlayerShelf(
                playerColors = playerColors,
                shelfSharedViewModel = shelfSharedViewModel,
                playerViewModel = viewModel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun TranscriptContent(
        state: TranscriptsUiState,
        isVisible: Boolean,
        isPortraitPlayer: Boolean,
        modifier: Modifier = Modifier,
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = transcriptEnterTransition,
            exit = transcriptExitTransition,
            modifier = modifier,
        ) {
            val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            TranscriptPage(
                uiState = state,
                toolbarPadding = PaddingValues(horizontal = 16.dp),
                paywallPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                transcriptPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navigationBarPadding + if (isPortraitPlayer) 96.dp else 16.dp),
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
                onShowTranscriptPaywall = {
                    transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN)
                },
                toolbarTrailingContent = { toolbarColors ->
                    if (state.isTextTranscriptLoaded && FeatureFlag.isEnabled(Feature.SHARE_TRANSCRIPTS)) {
                        TranscriptShareButton(
                            toolbarColors = toolbarColors,
                            onClick = transcriptViewModel::shareTranscript,
                        )
                    }
                },
            )
        }
    }

    @Composable
    private fun AdAndArtwork(
        ad: BlazeAd?,
        artworkOrVideoState: ArtworkOrVideoState,
        isTranscriptOpen: Boolean,
        playerColors: PlayerColors,
        modifier: Modifier = Modifier,
    ) {
        SubcomposeLayout(modifier = modifier) { constraints ->
            val adPlaceable = if (!isTranscriptOpen) {
                subcompose("ad") {
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
                }.getOrNull(0)?.measure(constraints.copyMaxDimensions())
            } else {
                null
            }

            val spacingDp = 16.dp
            val spacingPx = spacingDp.roundToPx()
            val availableHeightPx = constraints.maxHeight - (adPlaceable?.height ?: 0) - spacingPx
            val artworkPlaceable = if (!isTranscriptOpen && availableHeightPx > 64.dp.roundToPx()) {
                val availableHeightDp = availableHeightPx.toDp()
                val artworkConstraints = Constraints(maxHeight = availableHeightPx, maxWidth = constraints.maxWidth)
                subcompose("artworkOrVideo") {
                    ArtworkOrVideo(
                        state = artworkOrVideoState,
                        artworkCornerRadius = if (availableHeightDp > 120.dp) 16.dp else 8.dp,
                        onChapterUrlClick = viewModel::onChapterUrlClick,
                        configureVideoView = { videoView ->
                            videoView.setOnClickListener { onFullScreenVideoClick() }
                        },
                        modifier = Modifier.padding(top = spacingDp),
                    )
                }[0].measure(artworkConstraints)
            } else {
                null
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val placeablesHeight = (adPlaceable?.height ?: 0) + (artworkPlaceable?.height ?: 0)
                val yOffset = (constraints.maxHeight - placeablesHeight) / 2

                adPlaceable?.let { placeable ->
                    placeable.place((constraints.maxWidth - adPlaceable.width) / 2, yOffset)
                }
                artworkPlaceable?.let { placeable ->
                    placeable.place((constraints.maxWidth - placeable.width) / 2, yOffset + (adPlaceable?.height ?: 0))
                }
            }
        }
    }

    @Composable
    private fun AdAndArtworkHorizontal(
        artworkOrVideoState: ArtworkOrVideoState,
        playerColors: PlayerColors,
        modifier: Modifier = Modifier,
    ) {
        when (artworkOrVideoState) {
            is ArtworkOrVideoState.Artwork -> {
                Row(
                    modifier = modifier,
                ) {
                    ArtworkImage(
                        state = artworkOrVideoState.artworkImageState,
                        cornerRadius = 8.dp,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(
                        modifier = Modifier.width(16.dp),
                    )
                    EpisodeTitles(
                        playerColors = playerColors,
                        playerViewModel = viewModel,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            is ArtworkOrVideoState.Video -> {
                Column(
                    modifier = modifier,
                ) {
                    VideoBox(
                        player = artworkOrVideoState.player,
                        configureVideoView = { videoView ->
                            videoView.setOnClickListener { onFullScreenVideoClick() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    EpisodeTitles(
                        playerColors = playerColors,
                        playerViewModel = viewModel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is ArtworkOrVideoState.NoContent -> {
                EpisodeTitles(
                    playerColors = playerColors,
                    playerViewModel = viewModel,
                    textAlign = TextAlign.Start,
                    modifier = modifier,
                )
            }
        }
    }

    @Composable
    private fun PlayerSeekBar(
        headerData: PlayerViewModel.PlayerHeader,
        playerColors: PlayerColors,
        modifier: Modifier = Modifier,
    ) {
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
            modifier = modifier,
        )
    }

    @Composable
    private fun updateTranscriptTransitionData(
        isTranscriptOpen: Boolean,
        isTranscriptPaywallOpen: Boolean,
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
        val showPlayerControls = rememberUpdatedState(if (transcriptOpenState.value) !isTranscriptPaywallOpen else true)

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

    @Composable
    private fun AdImpressionEffect(
        ad: BlazeAd?,
        isPlayerOpen: Boolean,
    ) {
        LaunchedEffect(ad?.id, isPlayerOpen) {
            if (ad != null && isPlayerOpen) {
                viewModel.trackAdImpression(ad)
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
