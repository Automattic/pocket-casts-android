package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.tv.material3.TabRowDefaults
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
        containerColor = TvColors.Dark,
        indicator = @Composable { tabPositions, doesTabRowHaveFocus ->
            tabPositions.getOrNull(selectedTabIndex)?.let { currentTabPosition ->
                TabRowDefaults.PillIndicator(
                    currentTabPosition = currentTabPosition,
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.12f),
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedTabIndex,
                onFocus = { onTabSelect(index) },
                modifier = Modifier.height(24.dp),
                colors = TabDefaults.pillIndicatorTabColors(
                    contentColor = Color.White,
                    selectedContentColor = TvColors.Dark,
                    focusedContentColor = Color.White,
                    focusedSelectedContentColor = TvColors.Dark,
                    inactiveContentColor = Color.White.copy(alpha = 0.6f),
                ),
            ) {
                when (tab) {
                    is TvTab.TextTab -> {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    is TvTab.IconTab -> {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = stringResource(tab.contentDescriptionRes),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(horizontal = 4.dp),
                        )
                    }

                    is TvTab.TextWithIconTab -> {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
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
