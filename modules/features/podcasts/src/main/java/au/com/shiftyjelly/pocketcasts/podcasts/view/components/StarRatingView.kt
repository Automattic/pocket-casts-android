package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.runtime.Composable
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
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated

private const val MAX_STARS = 5
@Composable
fun StarRatingView(averageRating: Double, total: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stars(rating = averageRating, color = MaterialTheme.theme.colors.filter03)

        total?.let { TextP50(text = it.abbreviated) }
    }
}

@Composable
private fun Stars(
    rating: Double,
    color: Color,
) {
    // truncate the floating points off without rounding
    val stars = rating.toInt()
    // Get the float value
    val half = rating % 1

    Row(horizontalArrangement = Arrangement.Start) {
        for (index in 0 until MAX_STARS) {
            Icon(
                imageVector = iconFor(index, stars, half),
                contentDescription = null,
                tint = color
            )
        }
    }
}

private fun iconFor(index: Int, stars: Int, half: Double) = when {
    index < stars -> Icons.Filled.Star
    (index > stars) || (half < 0.5) -> Icons.Default.StarBorder
    else -> Icons.Filled.StarHalf
}

@Preview
@Composable
private fun PodcastRatingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        StarRatingView(
            averageRating = 3.75,
            total = 15071,
        )
    }
}
