package au.com.shiftyjelly.pocketcasts.compose.summary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.htmlEncode
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.theme
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
            .verticalScrollBar(scrollState = listState, thumbColor = scrollBarColor),
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
            HtmlText(
                html = markdownToHtml(text),
                color = textColor,
                textStyleResId = UR.style.P40,
            )
        }
    }
}

fun markdownToHtml(markdown: String): String {
    return markdown.lines()
        .joinToString("\n") { line ->
            when {
                line.startsWith("### ") -> "<h3>${line.removePrefix("### ").htmlEncode()}</h3>"
                line.startsWith("## ") -> "<h2>${line.removePrefix("## ").htmlEncode()}</h2>"
                line.startsWith("# ") -> "<h1>${line.removePrefix("# ").htmlEncode()}</h1>"
                line.startsWith("- ") -> "&#8226; ${line.removePrefix("- ").htmlEncode()}<br>"
                line.startsWith("* ") -> "&#8226; ${line.removePrefix("* ").htmlEncode()}<br>"
                line.isBlank() -> "<br>"
                else -> "${line.htmlEncode()}<br>"
            }
        }
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
}
