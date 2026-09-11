package au.com.shiftyjelly.pocketcasts.component

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoViewModel.ShowNotes
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvEpisodeInfoModal(
    episode: PodcastEpisode,
    actionContext: TvEpisodeActionContext,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvEpisodeInfoViewModel = hiltViewModel(),
) {
    LaunchedEffect(episode.uuid) {
        viewModel.trackDetailShown(actionContext.episodeViewSource)
        viewModel.load(episode.podcastUuid, episode.uuid)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState?.takeIf { it.episodeUuid == episode.uuid }
    TvModal(
        onDismissRequest = onDismissRequest,
        width = EpisodeInfoModalWidth,
        modifier = modifier,
    ) {
        TvEpisodeInfoModalContent(
            episode = episode,
            podcastTitle = state?.podcastTitle,
            showNotes = state?.showNotes ?: ShowNotes.Loading,
        )
    }
}

@Composable
private fun ColumnScope.TvEpisodeInfoModalContent(
    episode: PodcastEpisode,
    podcastTitle: String?,
    showNotes: ShowNotes,
) {
    val context = LocalContext.current
    EpisodeHeader(episode = episode, podcastTitle = podcastTitle, context = context)
    Text(
        text = stringResource(LR.string.tv_episode_details),
        style = MaterialTheme.tvTypography.callout,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.tvColors.textPrimary,
        modifier = Modifier.fillMaxWidth(),
    )
    EpisodeDescriptionPane(
        showNotes = showNotes,
        modifier = Modifier
            .fillMaxWidth()
            .height(DescriptionHeight),
    )
}

@Composable
private fun EpisodeHeader(
    episode: PodcastEpisode,
    podcastTitle: String?,
    context: Context,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TvArtworkImage(
            model = PodcastImage.getMediumArtworkUrl(episode.podcastUuid),
            modifier = Modifier
                .size(ArtworkSize)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.5.dp)) {
            if (!podcastTitle.isNullOrBlank()) {
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.tvTypography.caption2,
                    color = MaterialTheme.tvColors.textSecondary,
                )
            }
            Text(
                text = episode.title,
                style = MaterialTheme.tvTypography.body,
                color = MaterialTheme.tvColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = remember(episode.publishedDate, episode.durationMs) { episode.metadataLine(context) },
                style = MaterialTheme.tvTypography.caption2,
                color = MaterialTheme.tvColors.textSecondary,
            )
        }
    }
}

@Composable
private fun EpisodeDescriptionPane(
    showNotes: ShowNotes,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val scrollStep = with(LocalDensity.current) { 90.dp.toPx() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val isScrollable = showNotes !is ShowNotes.Loading
    Box(
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
            .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier),
    ) {
        when (showNotes) {
            is ShowNotes.Loading -> LoadingView(color = MaterialTheme.tvColors.textPrimary)

            is ShowNotes.Loaded -> Text(
                text = remember(showNotes.html) { AnnotatedString.fromHtml(showNotes.html) },
                style = MaterialTheme.tvTypography.caption2,
                color = MaterialTheme.tvColors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )

            is ShowNotes.Unavailable -> Text(
                text = stringResource(LR.string.error_loading_show_notes),
                style = MaterialTheme.tvTypography.caption2,
                color = MaterialTheme.tvColors.textSecondary,
            )
        }
    }
}

private fun PodcastEpisode.metadataLine(context: Context): String {
    val date = publishedDate.formatSkeleton("MMMMd")
    val duration = TimeHelper.getTimeDurationShortString(durationMs.toLong(), context, emptyString = "")
    return listOf(date, duration).filter { it.isNotBlank() }.joinToString(separator = " · ")
}

private fun Date.formatSkeleton(skeleton: String): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(this)
}

private val EpisodeInfoModalWidth = 570.dp
private val ArtworkSize = 72.dp
private val DescriptionHeight = 180.dp

@Preview
@Composable
private fun TvEpisodeInfoModalPreview() {
    TvTheme {
        TvModalSurface(width = EpisodeInfoModalWidth) {
            TvEpisodeInfoModalContent(
                episode = PodcastEpisode(
                    uuid = "episode-uuid",
                    title = "Cassandra Neyenesch Reads \"Enough for Now\"",
                    podcastUuid = "podcast-uuid",
                    duration = 1560.0,
                    publishedDate = Date(0),
                ),
                podcastTitle = "The Writer's Voice",
                showNotes = ShowNotes.Loaded(
                    html = "Our sense of smell is often dismissed as our less important than sight and hearing, " +
                        "but what if it's quietly shaping our memories, mood and long-term brain health?",
                ),
            )
        }
    }
}
