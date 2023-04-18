package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.Star
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated
import java.util.UUID

@Composable
fun StarRatingView(
    viewModel: PodcastRatingsViewModel,
) {
    val state by viewModel.stateFlow.collectAsState()

    when (state) {
        is RatingState.Loaded -> {
            val loadedState = state as RatingState.Loaded
            Content(
                state = loadedState,
                onClick = { viewModel.onRatingStarsTapped(loadedState.podcastUuid) },
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stars(
            stars = state.stars,
            color = MaterialTheme.theme.colors.filter03
        )
        state.total?.let { TextP50(text = it.abbreviated) }
    }
}

@Composable
private fun Stars(
    stars: List<Star>,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.Start) {
        stars.forEach { star ->
            Icon(
                imageVector = star.mapToIcon(),
                contentDescription = null,
                tint = color
            )
        }
    }
}

fun Star.mapToIcon() = when (this) {
    Star.FilledStar -> Icons.Filled.Star
    Star.HalfStar -> Icons.Default.StarHalf
    Star.BorderedStar -> Icons.Filled.StarBorder
}

@Preview
@Composable
private fun PodcastRatingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Content(
            state = RatingState.Loaded(
                podcastUuid = UUID.randomUUID().toString(),
                stars = listOf(
                    Star.FilledStar,
                    Star.FilledStar,
                    Star.FilledStar,
                    Star.HalfStar,
                    Star.BorderedStar,
                ),
                total = 1200
            ),
            onClick = {}
        )
    }
}
