package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingFragmentViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingFragmentViewModel.State
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

private const val ARG_PODCAST_UUID = "podcastUuid"

@AndroidEntryPoint
class GiveRatingFragment : BaseFragment() {

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

                val viewModel = hiltViewModel<GiveRatingFragmentViewModel>()

                LaunchedEffect(podcastUuid) {
                    viewModel.checkIfUserCanRatePodcast(
                        podcastUuid = podcastUuid,
                        onFailure = ::exitWithError
                    )
                }

                val state by viewModel.state.collectAsState()
                when (state) {
                    State.Loading -> GiveRatingLoadingScreen()
                    State.CanRate -> GiveRatingScreen()
                    State.MustListenMore -> GiveRatingListenMoreScreen()
                }
            }
        }
    }

    @Suppress("Deprecation")
    private fun exitWithError(message: String) {
        Timber.e(message)
        activity?.onBackPressed()
    }
}
