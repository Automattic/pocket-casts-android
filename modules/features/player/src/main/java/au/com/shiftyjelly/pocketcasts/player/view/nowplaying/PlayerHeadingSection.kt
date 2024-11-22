package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.view.ChapterProgressCircle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlayerHeadingSection(
    playerViewModel: PlayerViewModel,
) {
    val state by playerViewModel.listDataLive
        .map {
            PlayerHeadingSectionState(
                theme = it.podcastHeader.theme,
                episodeUuid = it.podcastHeader.episodeUuid,
                title = it.podcastHeader.title,
                podcastUuid = it.podcastHeader.podcastUuid,
                podcastTitle = it.podcastHeader.podcastTitle,
                chapter = it.podcastHeader.chapter,
                chapterSummary = it.podcastHeader.chapterSummary,
                chapterProgress = it.podcastHeader.chapterProgress,
                chapterTimeRemaining = it.podcastHeader.chapterTimeRemaining,
                isChaptersPresent = it.podcastHeader.isChaptersPresent,
                isFirstChapter = it.podcastHeader.isFirstChapter,
                isLastChapter = it.podcastHeader.isLastChapter,
            )
        }
        .observeAsState(PlayerHeadingSectionState())

    Content(
        state = state,
        onPreviousChapterClick = { playerViewModel.onPreviousChapterClick() },
        onNextChapterClick = { playerViewModel.onNextChapterClick() },
        onChapterTitleClick = { playerViewModel.onChapterTitleClick(it) },
        onPodcastTitleClick = { playerViewModel.onPodcastTitleClick(state.episodeUuid, state.podcastUuid) },
    ) {
        ChapterProgressCircle(
            progress = state.chapterProgress,
        )
    }
}

@Composable
private fun Content(
    state: PlayerHeadingSectionState,
    onPreviousChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    onChapterTitleClick: (Chapter) -> Unit,
    onPodcastTitleClick: () -> Unit,
    chapterProgressCircle: @Composable (() -> Unit),
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            if (state.isChaptersPresent) {
                ChapterPreviousButton(
                    onClick = onPreviousChapterClick,
                    enabled = !state.isFirstChapter,
                    alpha = if (state.isFirstChapter) 0.5f else 1f,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp)
                    .weight(1f),
            ) {
                TextH30(
                    text = state.title,
                    color = Color(ThemeColor.playerContrast01(state.theme)),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .then(if (state.isChaptersPresent) Modifier.clickable { state.chapter?.let { onChapterTitleClick(it) } } else Modifier),
                )

                if (!state.isChaptersPresent) {
                    state.podcastTitle?.takeIf { it.isNotBlank() }?.let {
                        Spacer(
                            modifier = Modifier.height(4.dp),
                        )

                        TextH50(
                            text = state.podcastTitle,
                            color = MaterialTheme.theme.colors.playerContrast02,
                            maxLines = 1,
                            modifier = Modifier
                                .clickable { onPodcastTitleClick() },
                        )
                    }
                }

                if (state.isChaptersPresent) {
                    state.chapterSummary.takeIf { it.isNotBlank() }?.let {
                        Spacer(
                            modifier = Modifier.height(4.dp),
                        )

                        TextH70(
                            text = it,
                            maxLines = 1,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (state.isChaptersPresent) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChapterNextWithChapterProgressButton(
                        onClick = onNextChapterClick,
                        enabled = !state.isLastChapter,
                        alpha = if (state.isLastChapter) 0.5f else 1f,
                        chapterProgressCircle = chapterProgressCircle,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextH70(
                        text = state.chapterTimeRemaining,
                        color = Color.White,
                        modifier = Modifier
                            .alpha(0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterProgressCircle(
    progress: Float,
) {
    AndroidView(
        modifier = Modifier
            .size(30.dp),
        factory = { context ->
            ChapterProgressCircle(context).apply {
                this.progress = progress
            }
        },
        update = { view ->
            view.progress = progress
        },
    )
}

@Composable
private fun ChapterPreviousButton(
    onClick: () -> Unit,
    enabled: Boolean,
    alpha: Float,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .alpha(alpha),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .wrapContentSize(),
        ) {
            Icon(
                painterResource(R.drawable.ic_chapter_skipbackwards),
                tint = Color.White,
                contentDescription = stringResource(LR.string.player_action_previous_chapter),
            )
        }
    }
}

@Composable
private fun ChapterNextWithChapterProgressButton(
    onClick: () -> Unit,
    enabled: Boolean,
    alpha: Float,
    chapterProgressCircle: @Composable () -> Unit,
) {
    val contentDescription = stringResource(LR.string.player_action_next_chapter)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .alpha(alpha)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            painterResource(R.drawable.ic_chapter_skipforward),
            tint = Color.White,
            contentDescription = null,
        )
        chapterProgressCircle()
    }
}

data class PlayerHeadingSectionState(
    val theme: Theme.ThemeType = Theme.ThemeType.DARK,
    val episodeUuid: String = "",
    val title: String = "",
    val podcastUuid: String? = null,
    val podcastTitle: String? = null,
    val chapter: Chapter? = null,
    val chapterSummary: String = "",
    val chapterProgress: Float = 0f,
    val chapterTimeRemaining: String = "",
    val isChaptersPresent: Boolean = false,
    val isFirstChapter: Boolean = false,
    val isLastChapter: Boolean = false,
)

@Preview
@Composable
private fun PlayerHeadingSectionPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            state = PlayerHeadingSectionState(
                theme = themeType,
                title = "A very looooooooooooong episode title",
                episodeUuid = "Episode UUID",
                podcastTitle = "Podcast Title",
                chapterSummary = "Chapter Summary",
                chapterProgress = 0.5f,
                chapterTimeRemaining = "1:23",
                isChaptersPresent = true,
                isFirstChapter = false,
                isLastChapter = false,
            ),
            onPreviousChapterClick = {},
            onNextChapterClick = {},
            onChapterTitleClick = {},
            onPodcastTitleClick = {},
            chapterProgressCircle = { ChapterProgressCircle(progress = 0.5f) },
        )
    }
}

@Preview
@Composable
private fun PlayerHeadingSectionWithoutChapterPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            state = PlayerHeadingSectionState(
                theme = themeType,
                title = "Episode title",
                episodeUuid = "Episode UUID",
                podcastTitle = "Podcast Title",
                chapterSummary = "Chapter Summary",
                chapterProgress = 0.5f,
                chapterTimeRemaining = "1:23",
                isChaptersPresent = false,
                isFirstChapter = false,
                isLastChapter = false,
            ),
            onPreviousChapterClick = {},
            onNextChapterClick = {},
            onChapterTitleClick = {},
            onPodcastTitleClick = {},
            chapterProgressCircle = { ChapterProgressCircle(progress = 0.5f) },
        )
    }
}
