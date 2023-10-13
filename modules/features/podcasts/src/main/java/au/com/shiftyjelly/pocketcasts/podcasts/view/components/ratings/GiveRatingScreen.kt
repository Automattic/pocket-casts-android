package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.GiveRatingViewModel.State.Loaded.Stars
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun GiveRatingScreen(
    state: GiveRatingViewModel.State.Loaded,
    setStars: (Stars) -> Unit,
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

            StarsRow(
                stars = state.stars,
                setStars = setStars,
            )
        }

        Spacer(Modifier.weight(1f))

        RowButton(
            text = stringResource(LR.string.submit),
            onClick = submitRating
        )
    }
}

@Composable
private fun StarsRow(
    stars: Stars,
    setStars: (Stars) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Star(
            starState = when (stars) {
                Stars.Zero -> StarState.Empty
                Stars.Half -> StarState.Half
                else -> StarState.Full
            },
            onClick = { setStars(Stars.One) },
        )

        Star(
            starState = when {
                stars <= Stars.One -> StarState.Empty
                stars == Stars.OneAndHalf -> StarState.Half
                else -> StarState.Full
            },
            onClick = { setStars(Stars.Two) },
        )

        Star(
            starState = when {
                stars <= Stars.Two -> StarState.Empty
                stars == Stars.TwoAndHalf -> StarState.Half
                else -> StarState.Full
            },
            onClick = { setStars(Stars.Three) },
        )

        Star(
            starState = when {
                stars <= Stars.Three -> StarState.Empty
                stars == Stars.ThreeAndHalf -> StarState.Half
                else -> StarState.Full
            },
            onClick = { setStars(Stars.Four) },
        )

        Star(
            starState = when {
                stars <= Stars.Four -> StarState.Empty
                stars == Stars.FourAndHalf -> StarState.Half
                else -> StarState.Full
            },
            onClick = { setStars(Stars.Five) },
        )
    }
}

@Composable
private fun Star(
    starState: StarState,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = starState.icon,
        tint = MaterialTheme.theme.colors.primaryIcon01,
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
    )
}

private enum class StarState(val icon: ImageVector) {
    Empty(Icons.Filled.StarBorder),
    Half(Icons.Default.StarHalf),
    Full(Icons.Filled.Star),
}

@Preview
@Composable
private fun PodcastRatingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        StarsRow(
            stars = Stars.TwoAndHalf,
            setStars = {},
        )
    }
}
