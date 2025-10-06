package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun SearchResultFilters(
    items: List<String>,
    selectedIndex: Int,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            SearchFilterPill(
                title = item,
                isSelected = selectedIndex == index,
                onSelected = {
                    onFilterSelected(item)
                }
            )
        }
    }
}

@Composable
private fun SearchFilterPill(
    title: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextH40(
        text = title,
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.background(
                        color = MaterialTheme.theme.colors.primaryInteractive01,
                        shape = RoundedCornerShape(100),
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.theme.colors.primaryText02,
                        shape = RoundedCornerShape(100)
                    )
                }
            )
            .clickable(onClick = onSelected)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        textAlign = TextAlign.Center,
        color = if (isSelected) MaterialTheme.theme.colors.primaryUi01 else MaterialTheme.theme.colors.primaryText01,
    )
}

@Preview
@Composable
private fun PreviewSearchResultFilters(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchResultFilters(
            items = listOf("Top Results", "Podcasts", "Episodes"),
            selectedIndex = 1,
            onFilterSelected = {}
        )
    }
}