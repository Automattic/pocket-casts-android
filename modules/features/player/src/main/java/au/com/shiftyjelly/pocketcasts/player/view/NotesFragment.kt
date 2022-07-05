package au.com.shiftyjelly.pocketcasts.player.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentNotesBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.NotesViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSecondsFromColonFormattedString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.extensions.cleanup
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class NotesFragment : BaseFragment() {

    @Inject lateinit var settings: Settings
    @Inject lateinit var playbackManager: PlaybackManager

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val viewModel: NotesViewModel by viewModels()
    private var binding: FragmentNotesBinding? = null
    private var webView: WebView? = null

    companion object {
        internal const val ARG_EPISODE_UUID = "episode_uuid"

        fun newInstance(episodeUuid: String): NotesFragment {
            return NotesFragment().apply {
                arguments = bundleOf(
                    ARG_EPISODE_UUID to episodeUuid
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) {
            viewModel.loadEpisode(it.first, it.second)

            binding?.root?.setBackgroundColor(it.second)
            binding?.showNotes?.setBackgroundColor(it.second)
        }

        binding = FragmentNotesBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner

        binding?.progressBar?.apply {
            setIndicatorColor(ThemeColor.playerContrast03(theme.activeTheme))
            trackColor = ThemeColor.playerContrast05(theme.activeTheme)
        }

        setupShowNotes()

        viewModel.showNotes.observe(viewLifecycleOwner) { (notes, inProgress) ->
            if (webView == null) {
                // If the webview has crashed we need to reinitialise it or
                // else it won't show any notes until the app is restarted
                setupShowNotes()
            }
            binding?.progressBar?.visibility = if (inProgress) View.VISIBLE else View.GONE
            loadShowNotes(notes)
        }

        binding?.viewModel = viewModel

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.cleanup()
        webView = null
    }

    private fun setupShowNotes() {
        Timber.d("Setting up notes fragment show notes webview")
        try {
            binding?.showNotes?.removeAllViews()

            webView = WebView(requireContext()).apply {
                settings.blockNetworkLoads = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.javaScriptEnabled = false
                settings.loadsImagesAutomatically = true
                isScrollbarFadingEnabled = false
                isVerticalScrollBarEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                        if (url == null) {
                            return false
                        }
                        if (url.startsWith("http://localhost/#playerJumpTo=")) {
                            val time = url.split("=").last()
                            jumpToTime(time)
                            return true
                        }
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

            binding?.showNotes?.addView(webView, 0, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        } catch (e: Exception) {
            val errorMessage = resources.getString(if (e.message?.contains("webview", ignoreCase = true) == true) LR.string.error_webview_not_installed else LR.string.error_loading_show_notes)
            binding?.showNotesErrorText?.text = errorMessage
            binding?.showNotesErrorText?.show()
        }
    }

    private fun jumpToTime(timeStr: String) {
        val timeInSeconds = timeStr.toSecondsFromColonFormattedString() ?: return

        Toast.makeText(context, "Skipping to $timeStr", Toast.LENGTH_SHORT).show()
        playbackManager.seekToTimeMs((timeInSeconds * 1000))
    }

    private fun loadShowNotes(showNotes: String) {
        Timber.d("Loading notes")
        webView?.loadDataWithBaseURL("file:///android_asset/", showNotes, "text/html", "UTF-8", null)
    }
}
