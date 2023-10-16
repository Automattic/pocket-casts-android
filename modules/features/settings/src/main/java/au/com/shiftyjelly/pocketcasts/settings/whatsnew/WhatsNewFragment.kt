package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.HeadphoneControlsSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.PlaybackSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WhatsNewFragment : BaseFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setBackgroundColor(Color.Transparent.toArgb())
            setContent {
                AppTheme(theme.activeTheme) {

                    CallOnce {
                        analyticsTracker.track(
                            AnalyticsEvent.WHATSNEW_SHOWN,
                            mapOf("version" to Settings.WHATS_NEW_VERSION_CODE)
                        )
                    }

                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    val onClose: () -> Unit = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    }
                    WhatsNewPage(
                        onConfirm = {
                            analyticsTracker.track(
                                AnalyticsEvent.WHATSNEW_CONFIRM_BUTTON_TAPPED,
                                mapOf("version" to Settings.WHATS_NEW_VERSION_CODE)
                            )
                            onClose()
                            performConfirmAction(it)
                        },
                        onClose = {
                            analyticsTracker.track(
                                AnalyticsEvent.WHATSNEW_DISMISSED,
                                mapOf("version" to Settings.WHATS_NEW_VERSION_CODE)
                            )
                            onClose()
                        },
                    )
                }
            }
        }

    private fun performConfirmAction(navigationState: NavigationState) {
        when (navigationState) {
            NavigationState.PlaybackSettings -> openFragment(PlaybackSettingsFragment.newInstance(scrollToAutoPlay = true))
            NavigationState.HeadphoneControlsSettings -> openFragment(HeadphoneControlsSettingsFragment())
            NavigationState.FullScreenPlayerScreen -> openPlayer()
            NavigationState.StartUpsellFlow -> startUpsellFlow()
        }
    }

    private fun openFragment(fragment: Fragment) {
        val fragmentHostListener = activity as? FragmentHostListener
            ?: throw IllegalStateException("Activity must implement FragmentHostListener")
        fragmentHostListener.addFragment(fragment)
    }

    private fun openPlayer() {
        val fragmentHostListener = activity as? FragmentHostListener
            ?: throw IllegalStateException("Activity must implement FragmentHostListener")
        fragmentHostListener.openPlayer()
    }

    private fun startUpsellFlow() {
        val source = OnboardingUpgradeSource.BOOKMARKS
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
            showPatronOnly = Feature.BOOKMARKS_ENABLED.tier == FeatureTier.Patron ||
                Feature.BOOKMARKS_ENABLED.isCurrentlyExclusiveToPatron()
        )
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    companion object {
        fun isWhatsNewNewerThan(versionCode: Int?): Boolean {
            return Settings.WHATS_NEW_VERSION_CODE > (versionCode ?: 0)
        }
    }
}
