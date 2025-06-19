package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModePan
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModeResize
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HackWeekEoYFragment : BaseFragment(), HasBackstack {

    private var webView: WebView? = null

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var sharingClient: SharingClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val coroutineScope = rememberCoroutineScope()

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
                    coroutineScope.launch {
                        sharingClient.share(SharingRequest.hackEoYScreenshot(it).build())
                    }
                },
                onWebViewCreated = { webView = it },
                onWebViewDisposed = { webView = null },
                modifier = Modifier.fillMaxSize()
                    .background(color = Color.Cyan)
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