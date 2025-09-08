package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun SmartEpisodeRow(
    episode: PodcastEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            EpisodeImage(
                episode = episode,
                useEpisodeArtwork = useEpisodeArtwork,
                placeholderType = PlaceholderType.Small,
                corners = 4.dp,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp)),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight(),
            ) {
                TextC70(
                    text = episode.rememberHeaderText(),
                )
                TextH40(
                    text = episode.title,
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
                TextH60(
                    text = episode.rememberTimeLeftText(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
        HorizontalDivider(
            startIndent = 12.dp,
        )
    }
}

@Composable
private fun PodcastEpisode.rememberHeaderText(): AnnotatedString {
    val context = LocalContext.current
    val formatter = remember(context) { RelativeDateFormatter(context) }
    return remember(playingStatus) {
        val tintColor = context.getThemeColor(UR.attr.primary_icon_01)
        val spannable = getSummaryText(formatter, tintColor, showDuration = false, context)
        spannable.toAnnotatedString()
    }
}

@Composable
private fun PodcastEpisode.rememberTimeLeftText(): String {
    val context = LocalContext.current
    return remember(playedUpToMs, durationMs, isInProgress, context) {
        TimeHelper.getTimeLeft(playedUpToMs, durationMs.toLong(), isInProgress, context).text
    }
}

@Preview
@Composable
private fun SmartEpisodeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SmartEpisodeRow(
            episode = PodcastEpisode(
                uuid = "uuid",
                title = "Episode Title",
                duration = 6000.0,
                publishedDate = Date(0),
            ),
            useEpisodeArtwork = false,
        )
    }
}
