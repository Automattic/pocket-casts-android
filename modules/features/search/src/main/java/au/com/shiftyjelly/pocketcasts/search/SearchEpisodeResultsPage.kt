package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar

@Composable
fun SearchEpisodeResultsPage(
    @Suppress("UNUSED_PARAMETER") viewModel: SearchViewModel,
    onBackClick: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = "All episodes",
            bottomShadow = true,
            onNavigationClick = { onBackClick() },
        )
    }
}
