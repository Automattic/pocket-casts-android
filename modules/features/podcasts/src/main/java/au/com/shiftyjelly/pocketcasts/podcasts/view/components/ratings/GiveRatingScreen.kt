package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.starsToRating

@Composable
fun GiveRatingScreen(
    state: GiveRatingViewModel.State.Loaded,
    setRating: (Double) -> Unit,
    submitRating: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 56.dp),
        ) {
            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isLandscape) {
                    PodcastCover(
                        uuid = state.podcastUuid,
                        coverWidth = 164.dp,
                    )
                }

                Spacer(Modifier.height(40.dp))

                TextH30(
                    text = stringResource(R.string.podcast_rate, state.podcastTitle),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(32.dp))

                SwipeableStars(
                    initialRate = state.previousRate?.let { starsToRating(it) },
                    onStarsChanged = setRating,
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            if (state.previousRate != state.currentSelectedRate) {
                RowButton(
                    text = stringResource(R.string.submit),
                    onClick = submitRating,
                    enabled = state.currentSelectedRate != GiveRatingViewModel.State.Loaded.Stars.Zero,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.theme.colors.primaryText01,
                        disabledBackgroundColor = MaterialTheme.theme.colors.primaryInteractive03,
                    ),
                )
            }
        }

        NavigationIconButton(
            iconColor = MaterialTheme.theme.colors.primaryText01,
            navigationButton = NavigationButton.Close,
            onNavigationClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )
    }
}
