package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.component.TvCategoryTile
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.tvDiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ContentPadding = PaddingValues(horizontal = 48.dp)

@Composable
fun TvSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: TvSearchViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val discoverRows by viewModel.discoverRows.collectAsStateWithLifecycle()

    TvSearchContent(
        query = query,
        categories = categories,
        discoverRows = discoverRows,
        onQueryChange = { query = it },
        modifier = modifier,
    )
}

@Composable
private fun TvSearchContent(
    query: String,
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(ContentPadding)) {
            Spacer(modifier = Modifier.height(40.dp))
            TvSearchField(
                query = query,
                onQueryChange = onQueryChange,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (categories.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ContentPadding)
                            .height(1.dp)
                            .background(MaterialTheme.tvColors.overlayBorder),
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    TvRow(
                        title = stringResource(LR.string.tv_search_browse_categories),
                        items = categories,
                        contentPadding = ContentPadding,
                        key = { it.id },
                    ) { category ->
                        TvCategoryTile(category = category, onClick = {})
                    }
                }
            }

            if (query.isBlank()) {
                discoverRows.forEach { row ->
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    tvDiscoverRow(
                        row = row,
                        onOpenPodcast = {},
                        onPlayEpisode = {},
                        contentPadding = ContentPadding,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchScreenPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchContent(
                query = "",
                categories = listOf(
                    DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                    DiscoverCategory(id = 2, name = "True Crime", icon = "", source = ""),
                    DiscoverCategory(id = 3, name = "Fiction", icon = "", source = ""),
                ),
                discoverRows = emptyList(),
                onQueryChange = {},
            )
        }
    }
}
