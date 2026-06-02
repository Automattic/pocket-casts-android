package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme

@Composable
fun TvScaffold(
    viewModel: TvScaffoldViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TvScaffoldContent(
        tabs = uiState.tabs,
        selectedTabIndex = uiState.selectedTabIndex,
        onTabSelected = viewModel::selectTab,
        modifier = modifier,
    )
}

@Composable
private fun TvScaffoldContent(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            TvTopBar(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                onProfileClick = {},
            )
            Box(modifier = Modifier.weight(1f)) {
                val currentTab = tabs.getOrElse(selectedTabIndex) { tabs.first() }
                TvTabPlaceholder(tab = currentTab)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvScaffoldPreview() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    TvScaffoldContent(
        tabs = TvTab.entries,
        selectedTabIndex = selectedIndex,
        onTabSelected = { selectedIndex = it },
    )
}
