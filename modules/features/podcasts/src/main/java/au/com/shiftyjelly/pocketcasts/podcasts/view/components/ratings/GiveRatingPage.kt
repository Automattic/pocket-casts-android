package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel

@Composable
fun GiveRatingPage(
    podcastUuid: String,
    viewModel: GiveRatingViewModel,
    submitRating: () -> Unit,
    onDismiss: () -> Unit,
    onUserSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
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
            onRatingUpdate = viewModel::setRating,
            submitRating = submitRating,
            onDismiss = onDismiss,
            onShow = {
                viewModel.trackOnGiveRatingScreenShown(currentState.podcastUuid)
            },
            modifier = modifier,
        )
        is GiveRatingViewModel.State.Loading -> GiveRatingLoadingScreen(
            modifier = modifier,
        )
        is GiveRatingViewModel.State.NotAllowedToRate -> GiveRatingNotAllowedToRate(
            state = currentState,
            onDismiss = onDismiss,
            onShow = {
                viewModel.trackOnNotAllowedToRateScreenShown(currentState.podcastUuid)
            },
            modifier = modifier,
        )
        is GiveRatingViewModel.State.ErrorWhenLoadingPodcast -> {
            GiveRatingErrorScreen(
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}
