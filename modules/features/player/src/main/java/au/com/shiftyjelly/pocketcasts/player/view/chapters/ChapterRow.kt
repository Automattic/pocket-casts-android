package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChapterRow(
    state: ChaptersViewModel.ChapterState,
    isTogglingChapters: Boolean,
    selectedCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onClick: () -> Unit,
    onUrlClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapter = state.chapter
    var selectedState by remember(chapter.index) { mutableStateOf(state.chapter.selected) }
    val textColor = getTextColor(state, isTogglingChapters)
    Box(
        modifier = modifier
            // use intrinsic height so the progress bar fills the height of the row
            .height(IntrinsicSize.Max)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        if (state is ChaptersViewModel.ChapterState.Playing) {
            ChapterProgressBar(progress = state.progress)
        }
        val isEnabled = !selectedState || selectedCount > 1
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    if (isTogglingChapters) {
                        if (isEnabled) {
                            val selected = !selectedState
                            selectedState = selected
                            onSelectionChange(selected, chapter)
                        }
                    } else {
                        onClick()
                    }
                }
                .padding(end = 12.dp),
        ) {
            AnimatedVisibility(visible = isTogglingChapters) {
                Checkbox(
                    enabled = isEnabled,
                    checked = selectedState,
                    onCheckedChange = { selected ->
                        selectedState = selected
                        onSelectionChange(selected, chapter)
                    },
                    colors = LocalChaptersTheme.current.checkbox,
                )
                Spacer(Modifier.width(8.dp))
            }
            TextH50(
                text = (chapter.uiIndex).toString(),
                color = textColor,
                modifier = Modifier
                    .padding(horizontal = 12.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextH50(
                text = chapter.title,
                color = textColor,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
            )
            Spacer(Modifier.width(4.dp))
            if (chapter.url != null && !isTogglingChapters) {
                LinkButton(
                    textColor = textColor,
                    onClick = onUrlClick,
                )
                Spacer(Modifier.width(8.dp))
            }
            val context = LocalContext.current
            val duration = remember(chapter.duration) { TimeHelper.getTimeDurationMediumString(chapter.duration, context, "") }
            TextH50(
                text = duration,
                color = textColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 60.dp),
            )
        }
    }
}

@Composable
private fun ChapterProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(LocalChaptersTheme.current.progressBackground),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction = progress)
                .fillMaxHeight()
                .background(LocalChaptersTheme.current.progress),
        )
    }
}

@Composable
private fun LinkButton(textColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalChaptersTheme.current.linkIconBackground)
            .size(24.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_link),
            tint = textColor,
            contentDescription = stringResource(LR.string.player_chapter_url),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun getTextColor(
    state: ChaptersViewModel.ChapterState,
    isTogglingChapters: Boolean,
) = if (isTogglingChapters) {
    if (state.chapter.selected) {
        LocalChaptersTheme.current.chapterTogglingSelected
    } else {
        LocalChaptersTheme.current.chapterTogglingDeselected
    }
} else {
    if (state is ChaptersViewModel.ChapterState.Played) {
        LocalChaptersTheme.current.chapterPlayed
    } else {
        LocalChaptersTheme.current.chapterNotPlayed
    }
}

@Preview(name = "Light")
@Composable
private fun ChapterRowLightPreview() = ChapterRowPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChapterRowDarkPreview() = ChapterRowPreview(Theme.ThemeType.DARK)

@Preview(name = "Rose")
@Composable
private fun ChapterRowRosePreview() = ChapterRowPreview(Theme.ThemeType.ROSE)

@Preview(name = "Indigo")
@Composable
private fun ChapterRowIndigoPreview() = ChapterRowPreview(Theme.ThemeType.INDIGO)

@Preview(name = "ExtraDark")
@Composable
private fun ChapterRowExtraDarkPreview() = ChapterRowPreview(Theme.ThemeType.EXTRA_DARK)

@Preview(name = "DarkContrast")
@Composable
private fun ChapterRowDarkContrastPreview() = ChapterRowPreview(Theme.ThemeType.DARK_CONTRAST)

@Preview(name = "LightContrast")
@Composable
private fun ChapterRowLightContrastPreview() = ChapterRowPreview(Theme.ThemeType.LIGHT_CONTRAST)

@Preview(name = "Electric")
@Composable
private fun ChapterRowElectricPreview() = ChapterRowPreview(Theme.ThemeType.ELECTRIC)

@Preview(name = "Classic")
@Composable
private fun ChapterRowClassicPreview() = ChapterRowPreview(Theme.ThemeType.CLASSIC_LIGHT)

@Preview(name = "Radioactive")
@Composable
private fun ChapterRowRadioactivePreview() = ChapterRowPreview(Theme.ThemeType.RADIOACTIVE)

@Composable
private fun ChapterRowPreview(theme: Theme.ThemeType) {
    val chapter = Chapter(
        title = "Chapter Title",
        startTime = Duration.ZERO,
        endTime = 62.seconds,
        url = "https://pocketcasts.com".toHttpUrlOrNull(),
        imagePath = null,
        index = 0,
        uiIndex = 5,
    )
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            Column {
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.Played(chapter = chapter.copy(title = "Played chapter")),
                    isTogglingChapters = false,
                    selectedCount = 2,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.Playing(chapter = chapter.copy(title = "Playing chapter"), progress = 0.5f),
                    isTogglingChapters = false,
                    selectedCount = 2,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.NotPlayed(chapter = chapter.copy(title = "Not played chapter")),
                    isTogglingChapters = false,
                    selectedCount = 2,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.NotPlayed(chapter = chapter.copy(title = "Selected chapter")),
                    isTogglingChapters = true,
                    selectedCount = 2,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.NotPlayed(chapter = chapter.copy(title = "Deselected chapter", selected = false)),
                    isTogglingChapters = true,
                    selectedCount = 2,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
                ChapterRow(
                    state = ChaptersViewModel.ChapterState.NotPlayed(chapter = chapter.copy(title = "Last selected chapter")),
                    isTogglingChapters = true,
                    selectedCount = 1,
                    onSelectionChange = { _, _ -> },
                    onClick = {},
                    onUrlClick = {},
                )
            }
        }
    }
}
