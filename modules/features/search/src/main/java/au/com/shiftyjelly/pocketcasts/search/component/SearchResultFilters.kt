package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FadeConfig
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val defaultFadeConfig = FadeConfig.Default.copy(
    showStartFade = false,
    showEndFade = true,
)

@Composable
fun SearchResultFilters(
    items: List<String>,
    selectedIndex: Int,
    onFilterSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fadeConfig: FadeConfig = defaultFadeConfig,
) {
    FadedLazyRow(
        fadeConfig = fadeConfig,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items) { index, item ->
            SearchFilterPill(
                title = item,
                isSelected = selectedIndex == index,
                onSelect = {
                    onFilterSelect(index)
                },
            )
        }
    }
}

@Composable
private fun SearchFilterPill(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextH40(
        text = title,
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(
                        color = MaterialTheme.theme.colors.primaryInteractive01,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.theme.colors.primaryIcon02,
                            shape = CircleShape,
                        )
                        .background(
                            color = MaterialTheme.colors.background,
                            shape = CircleShape,
                        )
                },
            )
            .clickable(onClick = onSelect)
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
            onFilterSelect = {},
        )
    }
}
