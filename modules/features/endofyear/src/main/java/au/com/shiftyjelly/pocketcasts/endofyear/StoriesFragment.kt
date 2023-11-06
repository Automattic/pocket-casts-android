package au.com.shiftyjelly.pocketcasts.endofyear

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseAppCompatDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class StoriesFragment : BaseAppCompatDialogFragment() {
    private val viewModel: StoriesViewModel by viewModels()
    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(Color.BLACK, true)
    private val source: StoriesSource
        get() = StoriesSource.fromString(arguments?.getString(ARG_SOURCE))

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = UR.style.WindowAnimationSlideTransition
        return dialog
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        val isTablet = Util.isTablet(requireContext())
        if (!isTablet) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_SHOWN, AnalyticsProp.storiesShown(source))
                    StoriesPage(
                        viewModel = viewModel,
                        onCloseClicked = {
                            analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED, AnalyticsProp.StoriesDismissed.closeButton)
                            dismiss()
                        },
                        onRetryClicked = {
                            viewModel.onRetryClicked()
                        },
                        onUpsellClicked = ::onUpsellClicked
                    )
                }
            }
        }
    }

    private fun onUpsellClicked() {
        val source = OnboardingUpgradeSource.END_OF_YEAR
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
        )
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    private object AnalyticsProp {
        private const val source = "source"
        object StoriesDismissed {
            val closeButton = mapOf(source to "close_button")
        }
        fun storiesShown(storiesSource: StoriesSource) = mapOf(source to storiesSource.value)
    }

    enum class StoriesSource(val value: String) {
        MODAL("modal"),
        PROFILE("profile"),
        USER_LOGIN("user_login"),
        UNKNOWN("unknown");
        companion object {
            fun fromString(source: String?) =
                StoriesSource.values().find { it.value == source } ?: UNKNOWN
        }
    }

    companion object {
        private const val ARG_SOURCE = "source"
        fun newInstance(source: StoriesSource) = StoriesFragment().apply {
            arguments = bundleOf(
                ARG_SOURCE to source.value,
            )
        }
    }
}
