package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingTappedSource
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.Star
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastRating(
    state: RatingState.Loaded,
    onClick: (RatingTappedSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starsContentDescription = stringResource(LR.string.podcast_star_rating_content_description)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(RatingTappedSource.STARS) }
            .semantics {
                this.contentDescription = starsContentDescription
            }
            .padding(8.dp),
    ) {
        Stars(
            stars = state.stars,
            color = MaterialTheme.theme.colors.primaryUi05Selected,
        )

        if (!state.noRatings) {
            TextP40(
                text = state.roundedAverage,
                modifier = Modifier.padding(start = 4.dp),
                fontWeight = FontWeight.W700,
            )
        }

        TextP40(
            text = if (state.noRatings) stringResource(R.string.no_ratings) else "(${state.total?.abbreviated})",
            modifier = Modifier.padding(start = 4.dp),
        )
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
