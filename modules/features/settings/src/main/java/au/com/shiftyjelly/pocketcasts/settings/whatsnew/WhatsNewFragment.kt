package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.PlaybackSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
                        title = stringResource(LR.string.whats_new_autoplay_title),
                        message = stringResource(LR.string.whats_new_autoplay_body),
                        confirmButtonTitle = stringResource(LR.string.whats_new_autoplay_enable_button),
                        closeButtonTitle = stringResource(LR.string.whats_new_autoplay_maybe_later_button),
                        header = { AutoPlayHeader() },
                        onConfirm = {
                            analyticsTracker.track(
                                AnalyticsEvent.WHATSNEW_CONFIRM_BUTTON_TAPPED,
                                mapOf("version" to Settings.WHATS_NEW_VERSION_CODE)
                            )
                            onClose()
                            performConfirmAction()
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

    private fun performConfirmAction() {
        val fragmentHostListener = activity as? FragmentHostListener
            ?: throw IllegalStateException("Activity must implement FragmentHostListener")
        val fragment = PlaybackSettingsFragment.newInstance(scrollToAutoPlay = true)
        fragmentHostListener.addFragment(fragment)
    }

    companion object {
        fun isWhatsNewNewerThan(versionCode: Int?): Boolean {
            return Settings.WHATS_NEW_VERSION_CODE > (versionCode ?: 0)
        }
    }
}
