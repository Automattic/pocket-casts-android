package au.com.shiftyjelly.pocketcasts.component

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPodcastInfoModal(
    podcast: Podcast,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        width = InfoModalWidth,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(InfoModalHeight),
        ) {
            PodcastInfoPane(
                podcast = podcast,
                modifier = Modifier.width(InfoPaneWidth),
            )
            PodcastDescriptionPane(
                podcast = podcast,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun PodcastInfoPane(
    podcast: Podcast,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier,
    ) {
        TvArtworkImage(
            model = PodcastImage.getMediumArtworkUrl(podcast.uuid),
            modifier = Modifier
                .size(InfoArtworkSize)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(
                label = stringResource(LR.string.tv_podcast_info_network),
                value = podcast.author.takeIf { it.isNotBlank() },
            )
            InfoRow(
                label = stringResource(LR.string.tv_podcast_info_website),
                value = podcast.getShortUrl().takeIf { it.isNotBlank() },
            )
            InfoRow(
                label = stringResource(LR.string.tv_podcast_info_schedule),
                value = podcast.displayableSchedule(context),
            )
            InfoRow(
                label = stringResource(LR.string.tv_podcast_info_next_episode),
                value = podcast.displayableNextEpisode(context, dateFormatter),
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String?,
) {
    if (value == null) {
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.tvTypography.caption2,
            color = MaterialTheme.tvColors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.tvTypography.caption2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.tvColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PodcastDescriptionPane(
    podcast: Podcast,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val scrollStep = with(LocalDensity.current) { 90.dp.toPx() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }
                val delta = when (event.key) {
                    Key.DirectionUp -> -scrollStep
                    Key.DirectionDown -> scrollStep
                    else -> return@onKeyEvent false
                }
                scope.launch { scrollState.animateScrollBy(delta) }
                true
            }
            .focusable()
            .verticalScroll(scrollState),
    ) {
        if (podcast.author.isNotBlank()) {
            Text(
                text = podcast.author,
                style = MaterialTheme.tvTypography.caption2,
                color = MaterialTheme.tvColors.textSecondary,
            )
        }
        Text(
            text = podcast.title,
            style = MaterialTheme.tvTypography.headline,
            color = MaterialTheme.tvColors.textPrimary,
        )
        if (podcast.podcastDescription.isNotBlank()) {
            Text(
                text = podcast.podcastDescription,
                style = MaterialTheme.tvTypography.caption2,
                color = MaterialTheme.tvColors.textSecondary,
            )
        }
    }
}

private fun Podcast.displayableSchedule(context: Context): String? {
    val stringId = when (episodeFrequency?.lowercase()) {
        "hourly" -> LR.string.tv_podcast_schedule_hourly
        "daily" -> LR.string.tv_podcast_schedule_daily
        "weekly" -> LR.string.tv_podcast_schedule_weekly
        "fortnightly" -> LR.string.tv_podcast_schedule_fortnightly
        "monthly" -> LR.string.tv_podcast_schedule_monthly
        else -> return null
    }
    return context.getString(stringId)
}

private fun Podcast.displayableNextEpisode(context: Context, dateFormatter: RelativeDateFormatter): String? {
    val expectedDate = estimatedNextEpisode ?: return null
    if (expectedDate.time <= 0) {
        return null
    }
    val now = System.currentTimeMillis()
    return when {
        expectedDate.time < now - 7 * DateUtils.DAY_IN_MILLIS -> null
        DateUtils.isToday(expectedDate.time) -> context.getString(LR.string.today)
        DateUtils.isToday(expectedDate.time - DateUtils.DAY_IN_MILLIS) -> context.getString(LR.string.tv_podcast_next_episode_tomorrow)
        expectedDate.time < now -> context.getString(LR.string.tv_podcast_next_episode_soon)
        else -> dateFormatter.format(expectedDate)
    }
}

private val InfoModalWidth = 516.dp
private val InfoModalHeight = 252.dp
private val InfoPaneWidth = 132.dp
private val InfoArtworkSize = 72.dp

@Preview
@Composable
private fun TvPodcastInfoModalPreview() {
    TvTheme {
        TvModalSurface(width = InfoModalWidth) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(InfoModalHeight),
            ) {
                PodcastInfoPane(
                    podcast = previewPodcast,
                    modifier = Modifier.width(InfoPaneWidth),
                )
                PodcastDescriptionPane(
                    podcast = previewPodcast,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

private val previewPodcast = Podcast(
    uuid = "podcast-uuid",
    title = "The Writer's Voice",
    author = "The New Yorker",
    podcastUrl = "https://www.newyorker.com/podcast/the-writers-voice",
    episodeFrequency = "weekly",
    podcastDescription = "New Yorker fiction writers read and discuss stories from the magazine's archive.",
)
