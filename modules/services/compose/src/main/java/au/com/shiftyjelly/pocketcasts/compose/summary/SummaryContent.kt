package au.com.shiftyjelly.pocketcasts.compose.summary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.text.markdownToHtml
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun SummaryContent(
    text: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.theme.colors.primaryText01,
    textColor: Color = MaterialTheme.theme.colors.primaryText02,
    scrollBarColor: Color = MaterialTheme.theme.colors.primaryIcon02,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
            .verticalScrollBar(scrollState = listState, thumbColor = scrollBarColor, contentPadding = contentPadding),
    ) {
        item {
            Text(
                text = stringResource(LR.string.episode_summary),
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        item {
            val html = remember(text) { markdownToHtml(text) }
            HtmlText(
                html = html,
                color = textColor,
                textStyleResId = UR.style.P40,
            )
        }
    }
}

@Preview
@Composable
private fun SummaryContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SummaryContent(
            text = "## Episode Highlights\n\n- First key point discussed\n- Second important topic\n- **Notable quote** from the guest\n\nThe hosts wrap up with final thoughts.",
        )
    }
}
