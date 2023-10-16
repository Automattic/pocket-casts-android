package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun GiveRatingScreen(
    state: GiveRatingViewModel.State.Loaded,
    setRating: (Double) -> Unit,
    submitRating: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        NavigationIconButton(
            iconColor = MaterialTheme.theme.colors.primaryText01,
            navigationButton = NavigationButton.Close,
            onNavigationClick = onDismiss,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            PodcastCover(
                uuid = state.podcastUuid,
                coverWidth = 164.dp,
            )

            Spacer(Modifier.height(40.dp))

            TextH30(
                text = stringResource(LR.string.podcast_rate, state.podcastTitle),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            SwipeableStars(
                onStarsChanged = setRating,
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        RowButton(
            text = stringResource(LR.string.submit),
            onClick = submitRating
        )
    }
}
