package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_PODCAST_UUID = "podcastUuid"

class GiveRatingFragment : BaseDialogFragment() {

    companion object {
        fun newInstance(podcastUuid: String) = GiveRatingFragment().apply {
            arguments = bundleOf(
                ARG_PODCAST_UUID to podcastUuid
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {

        val podcastUuid = arguments?.getString(ARG_PODCAST_UUID)
        if (podcastUuid == null) {
            exitWithError("${this@GiveRatingFragment::class.simpleName} is missing podcastUuid argument")
            return@apply
        }

        setContent {
            AppThemeWithBackground(theme.activeTheme) {

                val viewModel = hiltViewModel<GiveRatingViewModel>()

                val coroutineScope = rememberCoroutineScope()
                val context = requireContext()
                LaunchedEffect(podcastUuid) {
                    viewModel.checkIfUserCanRatePodcast(
                        podcastUuid = podcastUuid,
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
                        onFailure = ::exitWithError
                    )
                }

                val state = viewModel.state.collectAsState().value
                when (state) {
                    is GiveRatingViewModel.State.Loaded -> GiveRatingScreen(
                        state = state,
                        onDismiss = ::dismiss,
                        setStars = viewModel::setStars,
                        submitRating = {
                            viewModel.submitRating(
                                context = context,
                                onSuccess = {
                                    Toast.makeText(context, "TODO: Submit rating", Toast.LENGTH_LONG).show()
                                    dismiss()
                                },
                            )
                        },
                    )
                    GiveRatingViewModel.State.Loading -> GiveRatingLoadingScreen()
                }
            }
        }
    }

    private fun exitWithError(message: String) {
        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message)
        dismiss()
    }
}
