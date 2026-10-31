package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.text.rememberThemeAttributeColor
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.player.viewmodel.NotesViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSecondsFromColonFormattedString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.extensions.blockRemoteContentOnInProcessRenderer
import au.com.shiftyjelly.pocketcasts.views.extensions.cleanup
import au.com.shiftyjelly.pocketcasts.views.extensions.copyLinkOnLongPress
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlayerShowNotesLinkTappedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class NotesFragment : BaseFragment() {

    @Inject lateinit var settings: Settings

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var eventHorizon: EventHorizon

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val viewModel: NotesViewModel by viewModels()
    private var webView: WebView? = null
        set(value) {
            field = value
            webViewState = value
        }
    private var webViewState by mutableStateOf<WebView?>(null)
    private var header by mutableStateOf<NotesHeader?>(null)
    private var backgroundColor by mutableStateOf(Color.Black)
    private var isLoading by mutableStateOf(false)
    private var errorText by mutableStateOf<String?>(null)

    companion object {
        internal const val ARG_EPISODE_UUID = "episode_uuid"

        fun newInstance(episodeUuid: String): NotesFragment {
            return NotesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EPISODE_UUID, episodeUuid)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) {
            val (episode, backgroundColor) = it
            viewModel.loadEpisode(episode, backgroundColor)

            val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 0, durationMs = episode.durationMs.toLong(), inProgress = false, context = requireContext())
            this.backgroundColor = Color(backgroundColor)
            header = NotesHeader(
                title = episode.title,
                date = episode.publishedDate.toLocalizedFormatLongStyle(),
                duration = timeLeft.text,
                durationDescription = timeLeft.description,
            )
        }

        setupShowNotes()

        viewModel.showNotes.observe(viewLifecycleOwner) { state ->
            if (webView == null) {
                // If the webview has crashed we need to reinitialise it or
                // else it won't show any notes until the app is restarted
                setupShowNotes()
            }
            isLoading = state is ShowNotesState.Loading
            val notes = if (state is ShowNotesState.Loaded) state.showNotes else ""
            loadShowNotes(notes)
        }

        return contentWithoutConsumedInsets {
            val activeTheme = theme.activeTheme
            NotesPage(
                header = header,
                colors = NotesColors(
                    background = backgroundColor,
                    contrast01 = rememberThemeAttributeColor(UR.attr.player_contrast_01),
                    contrast02 = rememberThemeAttributeColor(UR.attr.player_contrast_02),
                    contrast05 = rememberThemeAttributeColor(UR.attr.player_contrast_05),
                    progressIndicator = Color(ThemeColor.playerContrast03(activeTheme)),
                    progressTrack = Color(ThemeColor.playerContrast05(activeTheme)),
                ),
                isLoading = isLoading,
                showNotesView = webViewState,
                errorText = errorText,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.cleanup()
        webView = null
    }

    private fun setupShowNotes() {
        Timber.d("Setting up notes fragment show notes webview")
        try {
            webView = WebView(requireContext()).apply {
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.javaScriptEnabled = false
                blockRemoteContentOnInProcessRenderer()
                isScrollbarFadingEnabled = false
                isVerticalScrollBarEnabled = false
                // stop the web view jumping after loading
                isFocusable = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                copyLinkOnLongPress()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        if (url.startsWith("http://localhost/#playerJumpTo=")) {
                            val time = url.split("=").last()
                            jumpToTime(time)
                            return true
                        }

                        eventHorizon.track(
                            PlayerShowNotesLinkTappedEvent(
                                episodeUuid = viewModel.episode.value?.uuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
                            ),
                        )
                        return IntentUtil.webViewShouldOverrideUrl(url, view.context)
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        LogBuffer.e(LogBuffer.TAG_CRASH, "Show notes fragment webview gone for episode ${viewModel.episode.value?.title}")
                        view.cleanup()
                        webView = null
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = resources.getString(if (e.message?.contains("webview", ignoreCase = true) == true) LR.string.error_webview_not_installed else LR.string.error_loading_show_notes)
            errorText = errorMessage
        }
    }

    private fun jumpToTime(timeStr: String) {
        val timeInSeconds = timeStr.toSecondsFromColonFormattedString() ?: return

        Toast.makeText(context, getString(LR.string.skipping_to, timeStr), Toast.LENGTH_SHORT).show()
        playbackManager.seekToTimeMs((timeInSeconds * 1000))
    }

    private fun loadShowNotes(showNotes: String) {
        Timber.d("Loading notes")
        webView?.loadDataWithBaseURL("file:///android_asset/", showNotes, "text/html", "UTF-8", null)
    }
}
