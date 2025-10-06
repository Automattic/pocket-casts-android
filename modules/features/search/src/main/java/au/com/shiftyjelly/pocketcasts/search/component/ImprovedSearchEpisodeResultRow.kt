package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.converter.SafeDate
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ImprovedSearchEpisodeResultRow(
    item: SearchAutoCompleteItem.Episode,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EpisodeImage(
            episode = PodcastEpisode(
                uuid = item.uuid,
                title = item.title,
                duration = item.duration,
                publishedDate = SafeDate(0),
            ),
            useEpisodeArtwork = false,
            corners = 4.dp,
            modifier = Modifier.shadow(elevation = 6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {

            val context = LocalContext.current
            val formattedDuration = remember(item.duration, context) { TimeHelper.getTimeDurationMediumString((item.duration * 1000).toInt(), context) }
            val dateFormatter = RelativeDateFormatter(context)
            val formattedPublishDate = remember(item.publishedAt, dateFormatter) { dateFormatter.format(item.publishedAt) }

            TextC70(
                text = formattedPublishDate,
            )
            TextH40(
                text = item.title,
                color = MaterialTheme.theme.colors.primaryText01,
            )
            TextH60(
                text = formattedDuration,
                color = MaterialTheme.theme.colors.secondaryText02,
                fontWeight = FontWeight.W600,
            )
        }
        Icon(
            painter = painterResource(IR.drawable.filter_play),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(1.dp, color = MaterialTheme.theme.colors.primaryInteractive01, shape = CircleShape)
                .clickable(onClick = onPlay),
            tint = MaterialTheme.theme.colors.primaryInteractive01,
        )
    }
}

@Preview
@Composable
private fun PreviewEpisodeResultRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchEpisodeResultRow(
            item = SearchAutoCompleteItem.Episode(
                uuid = "",
                title = "Episode title",
                duration = 320.0,
                podcastUuid = "",
                publishedAt = Date()
            ),
            onClick = {},
            onPlay = {}
        )
    }
}