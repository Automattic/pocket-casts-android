package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ImprovedSearchTermSuggestionRow(
    searchTerm: String,
    item: SearchAutoCompleteItem.Term,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.theme.colors.primaryText01
    val label = remember(searchTerm, item) {
        buildAnnotatedString {
            append(item.term)
            Regex(Regex.escape(searchTerm), RegexOption.IGNORE_CASE).findAll(item.term).forEach { result ->
                if (result.range.start < result.range.endInclusive + 1) {
                    addStyle(
                        style = SpanStyle(color = primaryColor),
                        start = result.range.first,
                        end = result.range.endInclusive + 1,
                    )
                }
            }
        }
    }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(IR.drawable.ic_search), contentDescription = null, tint = MaterialTheme.theme.colors.primaryText01)
        TextH40(
            text = label,
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Preview
@Composable
private fun PreviewSuggestionRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchTermSuggestionRow(
            searchTerm = "query",
            item = SearchAutoCompleteItem.Term(
                term = "Query this",
            ),
            onClick = {},
        )
    }
}
