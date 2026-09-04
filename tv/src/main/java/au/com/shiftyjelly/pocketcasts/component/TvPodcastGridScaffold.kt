package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.flow.first

@Composable
internal fun TvPodcastGridScaffold(
    itemKeys: List<Any>,
    modifier: Modifier = Modifier,
    title: String? = null,
    horizontalContentPadding: Dp = 42.dp,
    autoFocusFirstItem: Boolean = false,
    restoreFocusTrigger: Int = 0,
    itemContent: @Composable (index: Int, itemModifier: Modifier) -> Unit,
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.tvTypography.title2,
                color = MaterialTheme.tvColors.textPrimary,
                modifier = Modifier.padding(start = horizontalContentPadding, top = 40.dp, bottom = 0.dp),
            )
        }
        val gridState = rememberLazyGridState()
        var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
        val focusRequesters = remember(itemKeys.size) { List(itemKeys.size) { FocusRequester() } }
        val gridFocusRequester = remember { FocusRequester() }

        if (autoFocusFirstItem) {
            LaunchedEffect(Unit) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
                runCatching { focusRequesters.firstOrNull()?.requestFocus() }
            }
        }

        var isInitialComposition by remember { mutableStateOf(true) }
        LaunchedEffect(restoreFocusTrigger) {
            // Only restore on an actual trigger change, not when a fresh grid mounts with a
            // non-zero trigger inherited from a hoisted state holder.
            if (isInitialComposition) {
                isInitialComposition = false
            } else {
                runCatching { gridFocusRequester.requestFocus() }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = horizontalContentPadding, top = 20.dp, end = horizontalContentPadding, bottom = 32.dp),
            modifier = Modifier
                .focusRequester(gridFocusRequester)
                .focusGroup()
                .focusProperties {
                    onEnter = {
                        val visible = gridState.layoutInfo.visibleItemsInfo
                        val target = itemKeys.indexOfFirst { it.toString() == lastFocusedKey }
                            .takeIf { index -> index >= 0 && visible.any { it.index == index } }
                            ?: visible.firstOrNull()?.index
                        target?.let { runCatching { focusRequesters.getOrNull(it)?.requestFocus() } }
                    }
                },
        ) {
            items(
                count = itemKeys.size,
                key = { index -> itemKeys[index] },
            ) { index ->
                itemContent(
                    index,
                    Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                lastFocusedKey = itemKeys[index].toString()
                            }
                        },
                )
            }
        }
    }
}

private const val GRID_COLUMNS = 6
