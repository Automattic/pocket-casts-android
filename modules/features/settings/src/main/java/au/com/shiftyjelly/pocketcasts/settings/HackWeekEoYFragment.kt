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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.util.WebViewScreenshotCapture
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModePan
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModeResize
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HackWeekEoYFragment : BaseFragment(), HasBackstack {

    private var webView: WebView? = null

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var sharingClient: SharingClient

    @Inject lateinit var settings: Settings

    @Inject lateinit var screenshotCapture: WebViewScreenshotCapture

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val coroutineScope = rememberCoroutineScope()

        val bottomPadding by settings.bottomInset.collectAsState(0)
        val miniPlayerPadding = bottomPadding.pxToDp(LocalContext.current).dp

        val basePadding = 16.dp
        val totalPadding = basePadding + miniPlayerPadding

        AppThemeWithBackground(theme.activeTheme) {
            HackWeekEoYPage(
                onGoBack = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                isSubscribedCallback = {
                    userManager.getSignInState().map { it.isSignedIn }.blockingFirst(false)
                },
                onUpsell = {
                    OnboardingLauncher.openOnboardingFlow(
                        activity = requireActivity(),
                        onboardingFlow = OnboardingFlow.Upsell(OnboardingUpgradeSource.END_OF_YEAR),
                    )
                },
                onShareScreenshot = {
                    val activity = activity
                    val webView = webView

                    if (activity != null && webView != null) {
                        coroutineScope.launch {
                            val file = screenshotCapture.captureScreenshot(webView, activity)
                            file?.let {
                                sharingClient.share(SharingRequest.hackEoYScreenshot(it).build())
                            }
                        }
                    }
                },
                onWebViewCreated = { webView = it },
                onWebViewDisposed = { webView = null },
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = totalPadding),
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
