package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel

@Composable
fun GiveRatingNotAllowedToRate(
    state: GiveRatingViewModel.State.NotAllowedToRate,
    onDismiss: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        NavigationIconButton(
            iconColor = MaterialTheme.theme.colors.primaryText01,
            navigationButton = NavigationButton.Close,
            onNavigationClick = onDismiss,
            modifier = Modifier.align(Alignment.Start),
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
                text = stringResource(R.string.not_allowed_to_rate_title),
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(32.dp))

            TextP40(
                text = stringResource(R.string.not_allowed_to_rate_description),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W400,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        RowButton(
            text = stringResource(R.string.done),
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryText01,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GiveRatingNotAllowedToRatePreview() {
    val state = GiveRatingViewModel.State.NotAllowedToRate(
        podcastUuid = "sample-podcast-uuid",
    )
    GiveRatingNotAllowedToRate(
        state = state,
        onDismiss = {},
    )
}
