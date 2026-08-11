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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: TvSearchViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    TvSearchContent(
        query = query,
        categories = categories,
        onCharacter = { query += it },
        onSpace = { query += ' ' },
        onDelete = { query = query.dropLast(1) },
        modifier = modifier,
    )
}

@Composable
private fun TvSearchContent(
    query: String,
    categories: List<DiscoverCategory>,
    onCharacter: (Char) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 48.dp)) {
            Spacer(modifier = Modifier.height(40.dp))
            TvSearchField(query = query)
            Spacer(modifier = Modifier.height(40.dp))
            TvSearchKeyboard(
                onCharacter = onCharacter,
                onSpace = onSpace,
                onDelete = onDelete,
                onSubmit = {},
                autoFocus = true,
            )
        }

        if (categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(1.dp)
                    .background(MaterialTheme.tvColors.overlayBorder),
            )
            Spacer(modifier = Modifier.height(24.dp))
            TvRow(
                title = stringResource(LR.string.tv_search_browse_categories),
                items = categories,
                contentPadding = PaddingValues(horizontal = 48.dp),
                key = { it.id },
            ) { category ->
                TvCategoryTile(category = category, onClick = {})
            }
            Spacer(modifier = Modifier.height(40.dp))
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
                onCharacter = {},
                onSpace = {},
                onDelete = {},
            )
        }
    }
}
