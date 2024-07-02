package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.Star
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.UUID

@Composable
fun StarRatingView(
    fragmentManager: FragmentManager,
    viewModel: PodcastRatingsViewModel,
) {
    val state by viewModel.stateFlow.collectAsState()

    when (state) {
        is RatingState.Loaded -> {
            val loadedState = state as RatingState.Loaded
            Content(
                state = loadedState,
                onClick = {
                    viewModel.onRatingStarsTapped(
                        podcastUuid = loadedState.podcastUuid,
                        fragmentManager = fragmentManager,
                    )
                },
            )
        }
        is RatingState.Loading,
        is RatingState.Error,
        -> Unit // Do Nothing
    }
}

@Composable
private fun Content(
    state: RatingState.Loaded,
    onClick: () -> Unit,
) {
    val verticalPadding = if (FeatureFlag.isEnabled(Feature.GIVE_RATINGS)) 8.dp else 18.dp

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stars(
            stars = state.stars,
            color = MaterialTheme.theme.colors.filter03,
        )

        if (!state.noRatings) {
            TextP40(
                text = state.roundedAverage,
                modifier = Modifier
                    .padding(start = 4.dp),
                fontWeight = FontWeight.W700,
            )
        }

        TextP40(
            text = if (state.noRatings) stringResource(R.string.no_ratings) else "(${state.total?.abbreviated})",
            modifier = Modifier
                .padding(start = 4.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        if (FeatureFlag.isEnabled(Feature.GIVE_RATINGS)) {
            RowOutlinedButton(
                text = stringResource(R.string.rate_button),
                onClick = { onClick() },
                includePadding = false,
                fontSize = 16.sp,
                textPadding = 0.dp,
                fontWeight = FontWeight.W500,
                border = null,
                fullWidth = false,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.theme.colors.primaryText01,
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun Stars(
    stars: List<Star>,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
    ) {
        stars.forEach { star ->
            Icon(
                imageVector = star.icon,
                contentDescription = null,
                tint = color,
            )
        }
    }
}

@Preview
@Composable
private fun PodcastRatingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Content(
            state = RatingState.Loaded(
                PodcastRatings(
                    podcastUuid = UUID.randomUUID().toString(),
                    average = 3.5,
                    total = 1200,
                ),
            ),
            onClick = {},
        )
    }
}
