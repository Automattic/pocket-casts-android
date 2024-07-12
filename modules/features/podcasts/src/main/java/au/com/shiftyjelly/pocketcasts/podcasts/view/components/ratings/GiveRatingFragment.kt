package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_PODCAST_UUID = "podcastUuid"

class GiveRatingFragment : BaseDialogFragment() {

    private val viewModel: GiveRatingViewModel by viewModels()

    companion object {
        fun newInstance(podcastUuid: String) = GiveRatingFragment().apply {
            arguments = bundleOf(
                ARG_PODCAST_UUID to podcastUuid,
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        val podcastUuid = arguments?.getString(ARG_PODCAST_UUID)
        if (podcastUuid == null) {
            exitWithError("${this@GiveRatingFragment::class.simpleName} is missing podcastUuid argument")
            return@apply
        }

        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                val coroutineScope = rememberCoroutineScope()
                val context = requireContext()

                GiveRatingPage(
                    podcastUuid = podcastUuid,
                    viewModel = viewModel,
                    submitRating = {
                        coroutineScope.launch {
                            viewModel.submitRating(
                                podcastUuid = podcastUuid,
                                context = context,
                                onSuccess = {
                                    Toast.makeText(context, getString(LR.string.thank_you_for_rating), Toast.LENGTH_LONG).show()
                                    dismiss()
                                },
                                onError = {
                                    Toast.makeText(context, getString(LR.string.something_went_wrong_to_rate_this_podcast), Toast.LENGTH_LONG).show()
                                    dismiss()
                                },
                            )
                        }
                    },
                    onDismiss = ::dismiss,
                    onUserSignedOut = {
                        OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                        Toast.makeText(context, context.getString(LR.string.podcast_log_in_to_rate), Toast.LENGTH_LONG)
                            .show()
                        coroutineScope.launch {
                            // a short delay prevents the screen from flashing before the onboarding flow is shown
                            delay(1.seconds)
                            dismiss()
                        }
                    },
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        val state = viewModel.state.value

        if (state is GiveRatingViewModel.State.Loaded) {
            viewModel.trackOnDismissed(AnalyticsEvent.RATING_SCREEN_DISMISSED)
        } else if (state is GiveRatingViewModel.State.NotAllowedToRate) {
            viewModel.trackOnDismissed(AnalyticsEvent.NOT_ALLOWED_TO_RATE_SCREEN_DISMISSED)
        }
    }

    private fun exitWithError(message: String) {
        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message)
        dismiss()
    }
}
