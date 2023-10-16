package au.com.shiftyjelly.pocketcasts.player.view.chapters

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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChapterRow(
    state: ChaptersViewModel.ChapterState,
    onClick: () -> Unit,
    onUrlClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapter = state.chapter
    val textColor =
        if (state is ChaptersViewModel.ChapterState.Played) MaterialTheme.theme.colors.playerContrast04
        else MaterialTheme.theme.colors.playerContrast01
    Box(
        modifier = modifier
            // use intrinsic height so the progress bar fills the height of the row
            .height(IntrinsicSize.Max)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (state is ChaptersViewModel.ChapterState.Playing) {
            ChapterProgressBar(progress = state.progress)
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 12.dp)
        ) {
            TextH50(
                text = chapter.index.toString(),
                color = textColor
            )
            Spacer(Modifier.width(8.dp))
            TextH50(
                text = chapter.title,
                color = textColor,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            )
            Spacer(Modifier.width(4.dp))
            if (chapter.url != null) {
                LinkButton(
                    textColor = textColor,
                    onClick = onUrlClick
                )
                Spacer(Modifier.width(8.dp))
            }
            val context = LocalContext.current
            val duration = remember(chapter.duration) { TimeHelper.getTimeDurationMediumString(chapter.duration, context, "") }
            TextH50(
                text = duration,
                color = textColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 60.dp)
            )
        }
    }
}

@Composable
private fun ChapterProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.playerContrast06)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction = progress)
                .fillMaxHeight()
                .background(MaterialTheme.theme.colors.playerContrast05)
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
            .background(MaterialTheme.theme.colors.playerContrast05)
            .size(24.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_link),
            tint = textColor,
            contentDescription = stringResource(LR.string.player_chapter_url),
            modifier = Modifier.size(16.dp)
        )
    }
}

@ShowkaseComposable(name = "ChapterRow", group = "Chapter", styleName = "Default - DARK")
@Preview(name = "Dark")
@Composable
fun ChapterRowPreview() {
    val chapter = Chapter(
        title = "Chapter Title",
        startTime = 0,
        endTime = 62,
        url = "https://pocketcasts.com".toHttpUrlOrNull(),
        imagePath = null,
        mimeType = null,
        index = 1
    )
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        Column {
            ChapterRow(
                state = ChaptersViewModel.ChapterState.Played(chapter = chapter),
                onClick = {},
                onUrlClick = {}
            )
            ChapterRow(
                state = ChaptersViewModel.ChapterState.Playing(chapter = chapter, progress = 0.5f),
                onClick = {},
                onUrlClick = {}
            )
            ChapterRow(
                state = ChaptersViewModel.ChapterState.NotPlayed(chapter = chapter),
                onClick = {},
                onUrlClick = {}
            )
        }
    }
}
