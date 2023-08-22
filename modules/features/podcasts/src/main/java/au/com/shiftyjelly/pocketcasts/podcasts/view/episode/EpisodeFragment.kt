package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastAndEpisodeDetailsCoordinator
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSecondsFromColonFormattedString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.cleanup
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import au.com.shiftyjelly.pocketcasts.views.helper.ShowNotesFormatter
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class EpisodeFragment : BaseFragment() {
    companion object {

        private object AnalyticsProp {
            object Key {
                const val SOURCE = "source"
                const val EPISODE_UUID = "episode_uuid"
            }
        }

        fun newInstance(
            episodeUuid: String,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            podcastUuid: String? = null,
            fromListUuid: String? = null,
            forceDark: Boolean = false
        ): EpisodeFragment {
            return EpisodeFragment().apply {
                arguments = bundleOf(
                    EpisodeContainerFragment.ARG_EPISODE_UUID to episodeUuid,
                    EpisodeContainerFragment.ARG_EPISODE_VIEW_SOURCE to source.value,
                    EpisodeContainerFragment.ARG_OVERRIDE_PODCAST_LINK to overridePodcastLink,
                    EpisodeContainerFragment.ARG_PODCAST_UUID to podcastUuid,
                    EpisodeContainerFragment.ARG_FROMLIST_UUID to fromListUuid,
                    EpisodeContainerFragment.ARG_FORCE_DARK to forceDark
                )
            }
        }
    }

    override lateinit var statusBarColor: StatusBarColor

    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var warningsHelper: WarningsHelper
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Inject lateinit var podcastAndEpisodeDetailsCoordinator: PodcastAndEpisodeDetailsCoordinator

    private val viewModel: EpisodeFragmentViewModel by viewModels()
    private var binding: FragmentEpisodeBinding? = null
    private lateinit var imageLoader: PodcastImageLoader

    private var webView: WebView? = null
    private var formattedNotes: String? = null
    private lateinit var showNotesFormatter: ShowNotesFormatter

    private val episodeUUID: String?
        get() = arguments?.getString(EpisodeContainerFragment.ARG_EPISODE_UUID)

    private val episodeViewSource: EpisodeViewSource
        get() = EpisodeViewSource.fromString(arguments?.getString(EpisodeContainerFragment.ARG_EPISODE_VIEW_SOURCE))

    private val overridePodcastLink: Boolean
        get() = arguments?.getBoolean(EpisodeContainerFragment.ARG_OVERRIDE_PODCAST_LINK) ?: false

    val podcastUuid: String?
        get() = arguments?.getString(EpisodeContainerFragment.ARG_PODCAST_UUID)

    val fromListUuid: String?
        get() = arguments?.getString(EpisodeContainerFragment.ARG_FROMLIST_UUID)

    private val forceDarkTheme: Boolean
        get() = arguments?.getBoolean(EpisodeContainerFragment.ARG_FORCE_DARK) ?: false

    var listener: FragmentHostListener? = null
    private var episodeLoadedListener: EpisodeLoadedListener? = null

    val activeTheme: Theme.ThemeType
        get() = if (forceDarkTheme && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val themeResId = if (!forceDarkTheme || theme.isDarkTheme) {
            activeTheme.resourceId
        } else {
            R.style.ThemeDark
        }
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), themeResId)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        binding = FragmentEpisodeBinding.inflate(localInflater, container, false)

        showNotesFormatter = createShowNotesFormatter(contextThemeWrapper)

        statusBarColor = StatusBarColor.Custom(
            context?.getThemeColor(R.attr.primary_ui_01) ?: Color.WHITE, theme.isDarkTheme
        )
        return binding?.root
    }

    private fun createShowNotesFormatter(context: Context): ShowNotesFormatter {
        val showNotesFormatter = ShowNotesFormatter(context)
        showNotesFormatter.apply {
            setBackgroundThemeColor(UR.attr.primary_ui_01)
            setTextThemeColor(UR.attr.primary_text_01)
            setLinkThemeColor(UR.attr.primary_text_01)
            setConvertTimesToLinks(viewModel.isCurrentlyPlayingEpisode())
        }
        return showNotesFormatter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as FragmentHostListener
        episodeLoadedListener = (parentFragment as? EpisodeLoadedListener)
        imageLoader = PodcastImageLoaderThemed(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        episodeUUID?.let { episodeUuid ->
            podcastUuid?.let { podcastUuid ->
                if (!viewModel.isFragmentChangingConfigurations) {
                    analyticsTracker.track(AnalyticsEvent.EPISODE_DETAIL_SHOWN, mapOf(AnalyticsProp.Key.SOURCE to episodeViewSource.value))
                    FirebaseAnalyticsTracker.openedEpisode(podcastUuid, episodeUuid)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewModel.isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.EPISODE_DETAIL_DISMISSED, mapOf(AnalyticsProp.Key.SOURCE to episodeViewSource.value))
            podcastAndEpisodeDetailsCoordinator.onEpisodeDetailsDismissed()
        }
        webView.cleanup()
        webView = null
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.loadingGroup?.isInvisible = true

        viewModel.setup(episodeUUID!!, podcastUuid, forceDarkTheme)
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer
                when (state) {
                    is EpisodeFragmentState.Loaded -> {

                        binding.loadingGroup.isVisible = true
                        val iconColor = ThemeColor.podcastIcon02(activeTheme, state.tintColor)

                        episodeLoadedListener?.onEpisodeLoaded(
                            EpisodeToolbarState(
                                tintColor = iconColor,
                                episode = state.episode,
                                onFavClicked = { viewModel.starClicked() },
                                onShareClicked = { share(state) },
                            )
                        )

                        binding.episode = state.episode
                        binding.podcast = state.podcast
                        binding.tintColor = iconColor
                        binding.podcastColor = ThemeColor.podcastIcon02(activeTheme, state.podcastColor)

                        binding.btnDownload.tintColor = iconColor
                        binding.btnAddToUpNext.tintColor = iconColor
                        binding.btnArchive.tintColor = iconColor
                        binding.btnPlayed.tintColor = iconColor
                        binding.progressBar.progressTintList = ColorStateList.valueOf(state.podcastColor)

                        binding.webViewLoader.indeterminateDrawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(state.podcastColor, BlendModeCompat.SRC_IN)

                        binding.btnPlay.setCircleTintColor(iconColor)

                        // Played
                        binding.btnPlayed.isOn = state.episode.playingStatus == EpisodePlayingStatus.COMPLETED

                        // Archive
                        binding.btnArchive.isOn = state.episode.isArchived

                        // Time Left
                        val timeLeft = TimeHelper.getTimeLeft(state.episode.playedUpToMs, state.episode.durationMs.toLong(), state.episode.isInProgress, binding.lblTimeLeft.context)
                        binding.lblTimeLeft.text = timeLeft.text
                        binding.lblTimeLeft.contentDescription = timeLeft.description

                        // Download State
                        val downloadSize = Util.formattedBytes(bytes = state.episode.sizeInBytes, context = binding.btnDownload.context).replace(
                            "-",
                            getString(
                                LR.string.podcasts_download_download
                            )
                        )
                        val episodeStatus = state.episode.episodeStatus
                        binding.btnDownload.state = when (episodeStatus) {
                            EpisodeStatusEnum.NOT_DOWNLOADED -> DownloadButtonState.NotDownloaded(downloadSize)
                            EpisodeStatusEnum.QUEUED -> DownloadButtonState.Queued
                            EpisodeStatusEnum.DOWNLOADING -> DownloadButtonState.Downloading(state.downloadProgress)
                            EpisodeStatusEnum.DOWNLOAD_FAILED -> DownloadButtonState.Errored
                            EpisodeStatusEnum.DOWNLOADED -> DownloadButtonState.Downloaded(downloadSize)
                            else -> DownloadButtonState.Queued
                        }

                        val playbackError = state.episode.playErrorDetails

                        if (playbackError == null) {
                            binding.errorLayout.isVisible = episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED || episodeStatus == EpisodeStatusEnum.WAITING_FOR_POWER || episodeStatus == EpisodeStatusEnum.WAITING_FOR_WIFI
                            binding.lblErrorDetail.isVisible = false

                            binding.lblError.text = when (episodeStatus) {
                                EpisodeStatusEnum.DOWNLOAD_FAILED -> getString(LR.string.podcasts_download_failed)
                                EpisodeStatusEnum.WAITING_FOR_WIFI -> getString(LR.string.podcasts_download_wifi)
                                EpisodeStatusEnum.WAITING_FOR_POWER -> getString(LR.string.podcasts_download_power)
                                else -> null
                            }
                            if (episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                                binding.lblErrorDetail.text = state.episode.downloadErrorDetails
                                binding.lblErrorDetail.isVisible = true
                            }
                            val iconResource = when (episodeStatus) {
                                EpisodeStatusEnum.DOWNLOAD_FAILED -> IR.drawable.ic_failedwarning
                                EpisodeStatusEnum.WAITING_FOR_WIFI -> IR.drawable.ic_waitingforwifi
                                EpisodeStatusEnum.WAITING_FOR_POWER -> IR.drawable.ic_waitingforpower
                                else -> null
                            }
                            if (iconResource != null) {
                                binding.imgError.setImageResource(iconResource)
                            } else {
                                binding.imgError.setImageDrawable(null)
                            }
                        } else {
                            binding.errorLayout.isVisible = true
                            binding.lblError.setText(LR.string.podcast_episode_playback_error)
                            binding.lblErrorDetail.text = playbackError
                            binding.imgError.setImageResource(IR.drawable.ic_play_all)
                        }

                        // If we aren't showing another error we can show the episode limit warning
                        if (!state.episode.isArchived && !binding.errorLayout.isVisible && state.episode.excludeFromEpisodeLimit && state.podcast.autoArchiveEpisodeLimit != null) {
                            binding.errorLayout.isVisible = true
                            binding.lblErrorDetail.isVisible = true
                            binding.lblError.setText(LR.string.podcast_episode_manually_unarchived)
                            binding.lblErrorDetail.text = getString(LR.string.podcast_episode_manually_unarchived_summary, state.podcast.autoArchiveEpisodeLimit)
                            binding.imgError.setImageResource(IR.drawable.ic_archive)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Doesn't work in data binding for some reason maybe because of the API limit
                            binding.lblAuthor.compoundDrawableTintList = ColorStateList.valueOf(iconColor)
                        }
                        binding.lblAuthor.setOnClickListener {
                            analyticsTracker.track(
                                AnalyticsEvent.EPISODE_DETAIL_PODCAST_NAME_TAPPED,
                                mapOf(
                                    AnalyticsProp.Key.EPISODE_UUID to state.episode.uuid,
                                    AnalyticsProp.Key.SOURCE to EpisodeViewSource.PODCAST_SCREEN.value
                                )
                            )
                            (parentFragment as? BaseDialogFragment)?.dismiss()
                            if (!overridePodcastLink) {
                                (listener as FragmentHostListener).openPodcastPage(state.podcast.uuid)
                            }
                        }

                        binding.lblDate.setOnLongClickListener {
                            val message = "Added: ${state.episode.addedDate}\n" +
                                "Published: ${state.episode.publishedDate}\n" +
                                "Last Playback: ${state.episode.lastPlaybackInteractionDate}\n" +
                                "Last Download: ${state.episode.lastDownloadAttemptDate}"
                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton(LR.string.ok, null)
                                .show()
                            true
                        }
                        binding.podcastArtwork.let {
                            imageLoader.largePlaceholder().load(state.podcast).into(it)
                        }

                        binding.btnPlay.setOnPlayClicked {
                            val context = binding.root.context
                            val shouldClose = if (viewModel.shouldShowStreamingWarning(context)) {
                                warningsHelper.streamingWarningDialog(onConfirm = {
                                    val shouldCloseAfterWarning = viewModel.playClickedGetShouldClose(
                                        warningsHelper,
                                        force = true,
                                        fromListUuid = fromListUuid
                                    )
                                    if (shouldCloseAfterWarning) {
                                        (parentFragment as? BaseDialogFragment)?.dismiss()
                                    }
                                }).show(parentFragmentManager, "stream warning")
                                false
                            } else {
                                viewModel.playClickedGetShouldClose(warningsHelper, fromListUuid = fromListUuid)
                            }

                            if (shouldClose) {
                                (parentFragment as? BaseDialogFragment)?.dismiss()
                            }
                        }
                    }
                    is EpisodeFragmentState.Error -> {
                        Timber.e("Could not load episode $episodeUUID: ${state.error.message}")
                    }
                }
            }
        )

        viewModel.showNotesState.observe(viewLifecycleOwner) { showNotesState ->
            when (showNotesState) {
                is ShowNotesState.Loaded -> {
                    val showNotes = showNotesState.showNotes
                    formattedNotes = showNotesFormatter.format(showNotes) ?: showNotes
                    loadShowNotes(formattedNotes ?: "")
                }
                is ShowNotesState.Error, is ShowNotesState.NotFound -> {
                    formattedNotes = ""
                    loadShowNotes("")
                }
                is ShowNotesState.Loading -> {
                    // Do nothing as the starting state is loading
                }
            }
        }

        binding?.btnArchive?.let { button ->
            button.onStateChange = {
                viewModel.archiveClicked(button.isOn)
                if (button.isOn) {
                    (parentFragment as? BaseDialogFragment)?.dismiss()
                }
            }
        }

        binding?.btnPlayed?.let { button ->
            button.onStateChange = {
                viewModel.markAsPlayedClicked(button.isOn)
            }
        }

        // Up Next
        binding?.btnAddToUpNext?.setOnClickListener { _ ->
            val binding = binding ?: return@setOnClickListener
            if (!binding.btnAddToUpNext.isOn && viewModel.shouldShowUpNextDialog()) {
                val tintColor = ThemeColor.podcastIcon02(activeTheme, (viewModel.state.value as? EpisodeFragmentState.Loaded)?.tintColor ?: 0xFF000000.toInt())
                val dialog = OptionsDialog()
                    .setIconColor(tintColor)
                    .addCheckedOption(LR.string.play_next, imageId = IR.drawable.ic_upnext_playnext, click = { viewModel.addToUpNext(binding.btnAddToUpNext.isOn) })
                    .addCheckedOption(LR.string.play_last, imageId = IR.drawable.ic_upnext_playlast, click = { viewModel.addToUpNext(binding.btnAddToUpNext.isOn, addLast = true) })
                activity?.supportFragmentManager?.let {
                    dialog.show(it, "upnext")
                }
            } else {
                val wasAdded = viewModel.addToUpNext(binding.btnAddToUpNext.isOn)
                activity?.let { activity ->
                    val text = if (wasAdded) LR.string.episode_added_to_up_next else LR.string.episode_removed_from_up_next
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.inUpNext.observe(viewLifecycleOwner) { isInUpNext ->
            // Up Next
            binding?.btnAddToUpNext?.isOn = isInUpNext
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding?.btnPlay?.setPlaying(isPlaying = isPlaying, animate = true)
        }

        binding?.btnDownload?.setOnClickListener {
            val episode = viewModel.episode ?: return@setOnClickListener
            if (episode.isDownloaded) {
                val dialog = OptionsDialog()
                    .setTitle(getString(LR.string.podcast_remove_downloaded_file))
                    .addTextOption(
                        titleId = LR.string.podcast_file_remove,
                        titleColor = it.context.getThemeColor(UR.attr.support_05),
                        click = { viewModel.deleteDownloadedEpisode() }
                    )
                activity?.supportFragmentManager?.let { fragmentManager ->
                    dialog.show(fragmentManager, "confirm_archive_all")
                }
            } else {
                context?.let { context ->
                    if (settings.warnOnMeteredNetwork.value && !Network.isUnmeteredConnection(context) && viewModel.shouldDownload()) {
                        warningsHelper.downloadWarning(episodeUUID!!, "episode card")
                            .show(parentFragmentManager, "download warning")
                    } else {
                        viewModel.downloadEpisode()
                    }
                }
            }
        }

        binding?.btnAddToUpNext?.setup(ToggleActionButton.State.On(LR.string.podcasts_up_next, IR.drawable.ic_upnext_remove), ToggleActionButton.State.Off(LR.string.podcasts_up_next, IR.drawable.ic_upnext_playnext), false)
        binding?.btnPlayed?.setup(ToggleActionButton.State.On(LR.string.podcasts_mark_unplayed, IR.drawable.ic_markasunplayed), ToggleActionButton.State.Off(LR.string.podcasts_mark_played, IR.drawable.ic_markasplayed), false)
        binding?.btnArchive?.setup(ToggleActionButton.State.On(LR.string.podcasts_unarchive, IR.drawable.ic_unarchive), ToggleActionButton.State.Off(LR.string.podcasts_archive, IR.drawable.ic_archive), false)
    }

    private fun loadShowNotes(notes: String) {
        webView?.loadDataWithBaseURL("file://android_asset/", notes, "text/html", "UTF-8", null)
    }

    override fun onPause() {
        super.onPause()
        viewModel.isFragmentChangingConfigurations = activity?.isChangingConfigurations ?: false
    }

    override fun onStart() {
        super.onStart()

        // give the dialog a chance to open as the webview slows it down
        binding?.root?.postDelayed({ createShowNotesWebView() }, 300)
    }

    private fun createShowNotesWebView() {
        val context = this.context
        if (webView == null && context != null) {
            try {
                webView = WebView(context).apply {
                    settings.apply {
                        blockNetworkLoads = false
                        javaScriptCanOpenWindowsAutomatically = false
                        javaScriptEnabled = false
                        loadsImagesAutomatically = true
                    }
                    // stopping the white flash on web player load
                    setBackgroundColor(Color.argb(1, 0, 0, 0))
                    isVerticalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            if (url.startsWith("http://localhost/#playerJumpTo=")) {
                                val time = url.split("=").last()
                                jumpToTime(time)
                                return true
                            }

                            viewModel.episode?.uuid?.let { episodeUuid ->
                                analyticsTracker.track(
                                    AnalyticsEvent.EPISODE_DETAIL_SHOW_NOTES_LINK_TAPPED,
                                    mapOf(
                                        AnalyticsProp.Key.EPISODE_UUID to episodeUuid,
                                        AnalyticsProp.Key.SOURCE to EpisodeViewSource.PODCAST_SCREEN.value
                                    )
                                )
                            }

                            return IntentUtil.webViewShouldOverrideUrl(url, view.context)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            binding?.webViewLoader?.hide()
                            // fade in view
                            binding?.webViewShowNotes?.run {
                                visibility = View.VISIBLE
                            }
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            LogBuffer.e(LogBuffer.TAG_CRASH, "Episode fragment webview gone for episode ${viewModel.episode?.title}")
                            view.cleanup()
                            webView = null
                            return true
                        }
                    }
                }
                binding?.webViewShowNotes?.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            } catch (e: Exception) {
                Timber.e(e)
                binding?.webViewLoader?.hide()
                val errorMessage = resources.getString(if (e.message?.contains("webview", ignoreCase = true) == true) LR.string.error_webview_not_installed else LR.string.error_loading_show_notes)
                binding?.webViewErrorText?.text = errorMessage
                binding?.webViewErrorText?.show()
            }
        }
        formattedNotes?.let {
            loadShowNotes(it)
        }
    }

    private fun jumpToTime(timeStr: String) {
        val timeInSeconds = timeStr.toSecondsFromColonFormattedString() ?: return

        Toast.makeText(context, "Skipping to $timeStr", Toast.LENGTH_SHORT).show()
        viewModel.seekToTimeMs((timeInSeconds * 1000))
    }

    private fun share(state: EpisodeFragmentState.Loaded) {
        ShareDialog(
            state.podcast,
            state.episode,
            parentFragmentManager,
            context,
            shouldShowPodcast = false,
            analyticsTracker = analyticsTracker,
        ).show(sourceView = SourceView.EPISODE_DETAILS)
    }

    interface EpisodeLoadedListener {
        fun onEpisodeLoaded(state: EpisodeToolbarState)
    }

    data class EpisodeToolbarState(
        val tintColor: Int,
        val episode: PodcastEpisode,
        val onShareClicked: () -> Unit,
        val onFavClicked: () -> Unit,
    )
}
