package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text

@Composable
fun TvTabBar(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedTabIndex,
                onFocus = { onTabSelect(index) },
                colors = TabDefaults.pillIndicatorTabColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    selectedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface,
                    focusedSelectedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                val iconRes = tab.iconRes
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(tab.labelRes),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(horizontal = 4.dp),
                    )
                } else {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabBarPreview() {
    MaterialTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = selectedIndex,
            onTabSelect = { selectedIndex = it },
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabBarFirstSelectedPreview() {
    MaterialTheme {
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = 0,
            onTabSelect = {},
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabBarSearchSelectedPreview() {
    MaterialTheme {
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = 4,
            onTabSelect = {},
        )
    }
}
