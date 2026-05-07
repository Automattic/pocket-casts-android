package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SummaryBottomSheet : BaseDialogFragment() {

    companion object {
        private const val ARG_SUMMARY_TEXT = "summary_text"

        fun newInstance(summaryText: String): SummaryBottomSheet {
            return SummaryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUMMARY_TEXT, summaryText)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val summaryText = arguments?.getString(ARG_SUMMARY_TEXT).orEmpty()

        DialogBox {
            SummaryContent(
                text = summaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(rememberViewInteropNestedScrollConnection()),
            )
        }
    }
}

@Composable
private fun SummaryContent(
    text: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scrollBarColor = MaterialTheme.theme.colors.primaryIcon02

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        modifier = modifier
            .verticalScrollBar(scrollState = listState, thumbColor = scrollBarColor),
    ) {
        item {
            Text(
                text = stringResource(LR.string.episode_summary),
                color = MaterialTheme.theme.colors.primaryText01,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        item {
            HtmlText(
                html = markdownToHtml(text),
                color = MaterialTheme.theme.colors.primaryText02,
                textStyleResId = UR.style.P40,
            )
        }
    }
}

private fun markdownToHtml(markdown: String): String {
    return markdown.lines()
        .joinToString("\n") { line ->
            when {
                line.startsWith("### ") -> "<h3>${TextUtils.htmlEncode(line.removePrefix("### "))}</h3>"
                line.startsWith("## ") -> "<h2>${TextUtils.htmlEncode(line.removePrefix("## "))}</h2>"
                line.startsWith("# ") -> "<h1>${TextUtils.htmlEncode(line.removePrefix("# "))}</h1>"
                line.startsWith("- ") -> "&#8226; ${TextUtils.htmlEncode(line.removePrefix("- "))}<br>"
                line.startsWith("* ") -> "&#8226; ${TextUtils.htmlEncode(line.removePrefix("* "))}<br>"
                line.isBlank() -> "<br>"
                else -> "${TextUtils.htmlEncode(line)}<br>"
            }
        }
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
}
