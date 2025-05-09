package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.ButtonTab
import au.com.shiftyjelly.pocketcasts.compose.buttons.ButtonTabs
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter.TabsHeader
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class TabsViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(tabsHeader: TabsHeader) {
        val tabs = PodcastViewModel.PodcastTab.entries
            .map {
                ButtonTab(
                    labelResId = it.labelResId,
                    onClick = { tabsHeader.onTabClicked(it) },
                )
            }
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                ButtonTabs(
                    tabs = tabs,
                    selectedTab = tabs[tabsHeader.selectedTab.ordinal],
                    modifier = Modifier
                        .background(color = MaterialTheme.theme.colors.primaryUi02)
                        .fillMaxWidth(),

                )
            }
        }
    }
}
