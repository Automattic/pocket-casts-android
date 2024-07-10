package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel

@Composable
fun GiveRatingPage(
    podcastUuid: String,
    viewModel: GiveRatingViewModel,
    submitRating: () -> Unit,
    onDismiss: () -> Unit,
    onUserSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(podcastUuid) {
        viewModel.checkIfUserCanRatePodcast(
            podcastUuid,
            onUserSignedOut,
            onSuccess = {
                viewModel.loadData(podcastUuid)
            },
        )
    }
    when (val currentState = state) {
        is GiveRatingViewModel.State.Loaded -> GiveRatingScreen(
            state = currentState,
            setRating = viewModel::setRating,
            submitRating = submitRating,
            onDismiss = onDismiss,
        )
        is GiveRatingViewModel.State.Loading -> GiveRatingLoadingScreen()
        is GiveRatingViewModel.State.NotAllowedToRate -> GiveRatingNotAllowedToRate(
            state = currentState,
            onDismiss = onDismiss,
        )
        is GiveRatingViewModel.State.ErrorWhenLoadingPodcast -> {
            GiveRatingErrorScreen(onDismiss)
        }
    }
}
