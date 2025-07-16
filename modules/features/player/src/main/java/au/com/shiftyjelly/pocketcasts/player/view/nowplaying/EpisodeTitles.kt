package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterSummaryData
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeTitles(
    playerColors: PlayerColors,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val state by remember {
        playerViewModel.listDataLive.map {
            PlayerHeadingSectionState(
                episodeUuid = it.podcastHeader.episodeUuid,
                title = it.podcastHeader.title,
                podcastUuid = it.podcastHeader.podcastUuid,
                podcastTitle = it.podcastHeader.podcastTitle,
                chapter = it.podcastHeader.chapter,
                chapterSummary = it.podcastHeader.chapterSummary,
                chapterTimeRemaining = it.podcastHeader.chapterTimeRemaining,
                isChaptersPresent = it.podcastHeader.isChaptersPresent,
                isFirstChapter = it.podcastHeader.isFirstChapter,
                isLastChapter = it.podcastHeader.isLastChapter,
                chapterProgress = it.podcastHeader.chapterProgress,
            )
        }
    }.observeAsState(PlayerHeadingSectionState())

    Content(
        state = state,
        playerColors = playerColors,
        onPreviousChapterClick = { playerViewModel.onPreviousChapterClick() },
        onNextChapterClick = { playerViewModel.onNextChapterClick() },
        onChapterTitleClick = { playerViewModel.onChapterTitleClick(it) },
        onPodcastTitleClick = { playerViewModel.onPodcastTitleClick(state.episodeUuid, state.podcastUuid) },
        textAlign = textAlign,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Content(
    state: PlayerHeadingSectionState,
    onPreviousChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    onChapterTitleClick: (Chapter) -> Unit,
    onPodcastTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    playerColors: PlayerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault(),
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = playerColors.contrast01),
    ) {
        Row(
            modifier = modifier,
        ) {
            if (state.isChaptersPresent) {
                ChapterPreviousButton(
                    enabled = !state.isFirstChapter,
                    alpha = if (state.isFirstChapter) 0.5f else 1f,
                    iconTint = playerColors.contrast01,
                    onClick = onPreviousChapterClick,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = if (state.isChaptersPresent) 8.dp else 0.dp)
                    .weight(1f),
            ) {
                TextH30(
                    text = state.title,
                    color = playerColors.contrast01,
                    textAlign = textAlign,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (state.isChaptersPresent) {
                                Modifier.clickable { state.chapter?.let { onChapterTitleClick(it) } }
                            } else {
                                Modifier
                            },
                        ),
                )

                if (!state.isChaptersPresent) {
                    state.podcastTitle?.takeIf { it.isNotBlank() }?.let {
                        Spacer(
                            modifier = Modifier.height(4.dp),
                        )

                        TextH50(
                            text = state.podcastTitle,
                            color = playerColors.contrast02,
                            textAlign = textAlign,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPodcastTitleClick() },
                        )
                    }
                }

                if (state.isChaptersPresent) {
                    state.chapterSummary.takeIf { it.currentIndex != -1 && it.size > 0 }?.let { summary ->
                        val chapterSummary = stringResource(LR.string.chapter_count_summary, summary.currentIndex, summary.size)
                        val contentDescription = "$chapterSummary ${pluralStringResource(id = LR.plurals.chapter, count = summary.size)}"

                        Spacer(
                            modifier = Modifier.height(4.dp),
                        )

                        TextH70(
                            text = chapterSummary,
                            color = playerColors.contrast02,
                            textAlign = textAlign,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { this.contentDescription = contentDescription },
                        )
                    }
                }
            }

            if (state.isChaptersPresent) {
                val timeRemainingContentDescription = stringResource(
                    LR.string.chapter_time_remaining_content_description,
                    formatTimeRemainingContentDescription(state.chapterTimeRemaining),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChapterNextButtonWithChapterProgressCircle(
                        enabled = !state.isLastChapter,
                        alpha = if (state.isLastChapter) 0.5f else 1f,
                        progress = state.chapterProgress,
                        iconTint = playerColors.contrast01,
                        progressTint = playerColors.contrast04,
                        onClick = onNextChapterClick,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextH70(
                        text = state.chapterTimeRemaining,
                        color = playerColors.contrast02,
                        modifier = Modifier
                            .semantics { this.contentDescription = timeRemainingContentDescription }
                            .alpha(0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterPreviousButton(
    enabled: Boolean,
    alpha: Float,
    iconTint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.alpha(alpha),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chapter_skipbackwards),
                tint = iconTint,
                contentDescription = stringResource(LR.string.player_action_previous_chapter),
            )
        }
    }
}

@Composable
private fun ChapterNextButtonWithChapterProgressCircle(
    enabled: Boolean,
    progress: Float,
    alpha: Float,
    iconTint: Color,
    progressTint: Color,
    onClick: () -> Unit,
) {
    val contentDescription = stringResource(LR.string.player_action_next_chapter)
    IconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .alpha(alpha)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chapter_skipforward),
            tint = iconTint,
            contentDescription = null,
        )

        ChapterProgressCircle(
            progress = progress,
            tint = progressTint,
        )
    }
}

@Composable
private fun ChapterProgressCircle(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier.size(28.dp),
    ) {
        val degrees = 360f * (1f - progress)
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = -degrees,
            useCenter = false,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Butt),
        )
    }
}

@Composable
private fun formatTimeRemainingContentDescription(
    chapterTimeRemaining: String,
) = runCatching {
    val duration = Duration.parse(chapterTimeRemaining)
    when {
        duration.inWholeHours > 0 -> pluralStringResource(LR.plurals.hour, duration.inWholeHours.toInt())
        duration.inWholeMinutes > 0 -> pluralStringResource(LR.plurals.minute, duration.inWholeMinutes.toInt())
        else -> pluralStringResource(LR.plurals.second, duration.inWholeSeconds.toInt())
    }
}.getOrElse { chapterTimeRemaining }

private data class PlayerHeadingSectionState(
    val episodeUuid: String = "",
    val title: String = "",
    val podcastUuid: String? = null,
    val podcastTitle: String? = null,
    val chapter: Chapter? = null,
    val chapterSummary: ChapterSummaryData = ChapterSummaryData(),
    val chapterTimeRemaining: String = "",
    val isChaptersPresent: Boolean = false,
    val isFirstChapter: Boolean = false,
    val isLastChapter: Boolean = false,
    val chapterProgress: Float = 0.5f,
)

@Preview
@Composable
private fun PlayerHeadingSectionPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    val state = PlayerHeadingSectionState(
        title = "Episode title",
        episodeUuid = "Episode UUID",
        podcastTitle = "Podcast Title",
        chapterSummary = ChapterSummaryData(1, 5),
        chapterTimeRemaining = "1:23",
        isChaptersPresent = false,
        isFirstChapter = false,
        isLastChapter = false,
    )

    AppTheme(themeType) {
        CompositionLocalProvider(
            LocalPodcastColors provides PodcastColors.TheDailyPreview,
        ) {
            val playerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.background(playerColors.background01),
            ) {
                Content(
                    state = state,
                    onPreviousChapterClick = {},
                    onNextChapterClick = {},
                    onChapterTitleClick = {},
                    onPodcastTitleClick = {},
                )

                Content(
                    state = state.copy(
                        title = "Episode title / ".repeat(10),
                        podcastTitle = "Podcast title / ".repeat(10),
                    ),
                    onPreviousChapterClick = {},
                    onNextChapterClick = {},
                    onChapterTitleClick = {},
                    onPodcastTitleClick = {},
                )

                Content(
                    state = state.copy(isChaptersPresent = true),
                    onPreviousChapterClick = {},
                    onNextChapterClick = {},
                    onChapterTitleClick = {},
                    onPodcastTitleClick = {},
                )

                Content(
                    state = state.copy(
                        title = "Episode title / ".repeat(10),
                        isChaptersPresent = true,
                    ),
                    onPreviousChapterClick = {},
                    onNextChapterClick = {},
                    onChapterTitleClick = {},
                    onPodcastTitleClick = {},
                )
            }
        }
    }
}
