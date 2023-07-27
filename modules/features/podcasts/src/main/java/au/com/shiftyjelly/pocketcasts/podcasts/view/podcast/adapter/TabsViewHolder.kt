package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter.TabsHeader
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class TabsViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(tabsHeader: TabsHeader) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                TabsRow(tabsHeader)
            }
        }
    }
}

@Composable
private fun TabsRow(
    tabsHeader: TabsHeader,
) {
    val selectedTabIndex = tabsHeader.selectedTab.ordinal
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = MaterialTheme.theme.colors.primaryUi02,
        contentColor = MaterialTheme.theme.colors.primaryText01,
        edgePadding = 0.dp,
        divider = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        PodcastTab.values().toList()
            .map { stringResource(it.labelResId) }
            .forEachIndexed { i, text ->
                Tab(
                    selected = selectedTabIndex == i,
                    onClick = { tabsHeader.onTabClicked(PodcastTab.values()[i]) },
                    text = { au.com.shiftyjelly.pocketcasts.compose.components.TextH40(text = text) },
                )
            }
    }
}

@Preview
@Composable
private fun TabsRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        TabsRow(
            tabsHeader = TabsHeader(PodcastTab.EPISODES) {},
        )
    }
}
