package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.status.StatusFragment
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModePan
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModeResize
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HelpFragment : BaseFragment(), HasBackstack {
    @Inject
    lateinit var settings: Settings

    private var webView: WebView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppThemeWithBackground(theme.activeTheme) {
            HelpPage(
                activity = requireActivity(),
                onShowLogs = {
                    (requireActivity() as FragmentHostListener).addFragment(LogsFragment())
                },
                onShowStatusPage = {
                    (requireActivity() as FragmentHostListener).addFragment(StatusFragment())
                },
                onGoBack = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                onWebViewCreated = { webView = it },
                onWebViewDisposed = { webView = null },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 14.dp),
            )
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setupKeyboardModeResize()
    }

    override fun onDetach() {
        super.onDetach()
        setupKeyboardModePan()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView = null
    }

    override fun getBackstackCount(): Int {
        return if (webView?.canGoBack() == true) 1 else 0
    }

    override fun onBackPressed(): Boolean {
        return webView?.let {
            val canGoBack = it.canGoBack()
            if (canGoBack) {
                it.goBack()
            }
            canGoBack
        } ?: false
    }
}
