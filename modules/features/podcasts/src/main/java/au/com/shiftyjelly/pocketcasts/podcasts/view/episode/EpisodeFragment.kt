package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
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
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentEpisodeBinding
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
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
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import au.com.shiftyjelly.pocketcasts.views.helper.ShowNotesFormatter
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_EPISODE_UUID = "episodeUUID"
private const val ARG_EPISODE_VIEW_SOURCE = "episode_view_source"
private const val ARG_OVERRIDE_PODCAST_LINK = "override_podcast_link"
private const val ARG_PODCAST_UUID = "podcastUUID"
private const val ARG_FROMLIST_UUID = "fromListUUID"
private const val ARG_FORCE_DARK = "forceDark"

@AndroidEntryPoint
class EpisodeFragment : BaseDialogFragment() {
    companion object {

        private object AnalyticsProp {
            object Key {
                const val SOURCE = "source"
                const val EPISODE_UUID = "episode_uuid"
            }
        }
        fun newInstance(
            episode: Episode,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            fromListUuid: String? = null,
            forceDark: Boolean = false
        ): EpisodeFragment {
            return newInstance(
                episodeUuid = episode.uuid,
                source = source,
                overridePodcastLink = overridePodcastLink,
                podcastUuid = episode.podcastUuid,
                fromListUuid = fromListUuid,
                forceDark = forceDark
            )
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
                    ARG_EPISODE_UUID to episodeUuid,
                    ARG_EPISODE_VIEW_SOURCE to source.value,
                    ARG_OVERRIDE_PODCAST_LINK to overridePodcastLink,
                    ARG_PODCAST_UUID to podcastUuid,
                    ARG_FROMLIST_UUID to fromListUuid,
                    ARG_FORCE_DARK to forceDark
                )
            }
        }
    }

    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(
            context?.getThemeColor(UR.attr.primary_ui_01)
                ?: Color.WHITE,
            theme.isDarkTheme
        )

    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var warningsHelper: WarningsHelper
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: EpisodeFragmentViewModel by viewModels()
    private var binding: FragmentEpisodeBinding? = null
    private lateinit var imageLoader: PodcastImageLoader

    private var webView: WebView? = null
    private var formattedNotes: String? = null
    private lateinit var showNotesFormatter: ShowNotesFormatter

    val episodeUUID: String?
        get() = arguments?.getString(ARG_EPISODE_UUID)

    private val episodeViewSource: EpisodeViewSource
        get() = EpisodeViewSource.fromString(arguments?.getString(ARG_EPISODE_VIEW_SOURCE))

    val overridePodcastLink: Boolean
        get() = arguments?.getBoolean(ARG_OVERRIDE_PODCAST_LINK) ?: false

    val podcastUuid: String?
        get() = arguments?.getString(ARG_PODCAST_UUID)

    val fromListUuid: String?
        get() = arguments?.getString(ARG_FROMLIST_UUID)

    val forceDarkTheme: Boolean
        get() = arguments?.getBoolean(ARG_FORCE_DARK) ?: false

    var listener: FragmentHostListener? = null

    val activeTheme: Theme.ThemeType
        get() = if (forceDarkTheme && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (!forceDarkTheme || theme.isDarkTheme) {
            showNotesFormatter = createShowNotesFormatter(requireContext())
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        showNotesFormatter = createShowNotesFormatter(context)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    private fun createShowNotesFormatter(context: Context): ShowNotesFormatter {
        val showNotesFormatter = ShowNotesFormatter(settings, context)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEpisodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewModel.isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.EPISODE_DETAIL_DISMISSED, mapOf(AnalyticsProp.Key.SOURCE to episodeViewSource.value))
        }
        webView.cleanup()
        webView = null
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.behavior?.apply {
            isFitToContents = false
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        // Ensure the dialog ends up the full height of the screen
        // Bottom sheet dialogs get wrapped in a sheet that is WRAP_CONTENT so setting MATCH_PARENT on our
        // root view is ignored.
        bottomSheetDialog?.setOnShowListener {
            view.updateLayoutParams<ViewGroup.LayoutParams> {
                height = Resources.getSystem().displayMetrics.heightPixels
            }
        }

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

                        binding.episode = state.episode
                        binding.podcast = state.podcast
                        binding.tintColor = iconColor
                        binding.toolbarTintColor = iconColor
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
                            EpisodeStatusEnum.NOT_DOWNLOADED -> DownloadButton.State.NotDownloaded(downloadSize)
                            EpisodeStatusEnum.QUEUED -> DownloadButton.State.Queued
                            EpisodeStatusEnum.DOWNLOADING -> DownloadButton.State.Downloading(state.downloadProgress)
                            EpisodeStatusEnum.DOWNLOAD_FAILED -> DownloadButton.State.Errored
                            EpisodeStatusEnum.DOWNLOADED -> DownloadButton.State.Downloaded(downloadSize)
                            else -> DownloadButton.State.Queued
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

                        binding.btnShare.setOnClickListener {
                            share(state)
                        }

                        binding.btnFav.contentDescription = getString(if (state.episode.isStarred) LR.string.podcast_episode_starred else LR.string.podcast_episode_unstarred)

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
                            dismiss()
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
                                        dismiss()
                                    }
                                }).show(parentFragmentManager, "stream warning")
                                false
                            } else {
                                viewModel.playClickedGetShouldClose(warningsHelper, fromListUuid = fromListUuid)
                            }

                            if (shouldClose) {
                                dismiss()
                            }
                        }
                    }
                    is EpisodeFragmentState.Error -> {
                        Timber.e("Could not load episode $episodeUUID: ${state.error.message}")
                    }
                }
            }
        )

        // Ideally this would all be contained in the viewmodel state observable but webview flickers when updating
        viewModel.showNotes.observe(viewLifecycleOwner) { showNotes ->
            formattedNotes = showNotesFormatter.format(showNotes) ?: showNotes
            loadShowNotes(formattedNotes ?: "")
        }

        binding?.btnArchive?.let { button ->
            button.onStateChange = {
                viewModel.archiveClicked(button.isOn)
                if (button.isOn) {
                    dismiss()
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

        binding?.btnClose?.setOnClickListener { dismiss() }
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
                    if (settings.warnOnMeteredNetwork() && !Network.isUnmeteredConnection(context) && viewModel.shouldDownload()) {
                        warningsHelper.downloadWarning(episodeUUID!!, "episode card")
                            .show(parentFragmentManager, "download warning")
                    } else {
                        viewModel.downloadEpisode()
                    }
                }
            }
        }

        binding?.btnFav?.setOnClickListener { viewModel.starClicked() }

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
            shouldShowPodcast = false
        ).show()
    }
}
