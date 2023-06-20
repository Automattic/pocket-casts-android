package au.com.shiftyjelly.pocketcasts.wear.ui.filters

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.filters.FiltersViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object FiltersScreen {
    const val route = "filters_screen"
}

@Composable
fun FiltersScreen(
    onFilterTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FiltersViewModel = hiltViewModel(),
    columnState: ScalingLazyColumnState,
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) { // the state needs to be immutable or the following error will happen 'Smart cast is impossible'
        is UiState.Loaded -> Content(
            filters = state.filters,
            onFilterTap = onFilterTap,
            modifier = modifier,
            columnState = columnState,
        )
        is UiState.Loading -> LoadingScreen()
    }
}

@Composable
private fun Content(
    filters: List<Playlist>,
    onFilterTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    columnState: ScalingLazyColumnState,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        columnState = columnState
    ) {
        item {
            ScreenHeaderChip(LR.string.filters)
        }
        items(items = filters, key = { filter -> filter.uuid }) { filter ->
            WatchListChip(
                title = filter.title,
                onClick = { onFilterTap(filter.uuid) },
                icon = {
                    Icon(
                        painter = painterResource(filter.drawableId),
                        contentDescription = null,
                        tint = WearColors.getFilterColor(filter),
                        modifier = Modifier.padding(horizontal = 8.dp).size(24.dp)
                    )
                }
            )
        }
    }
}
