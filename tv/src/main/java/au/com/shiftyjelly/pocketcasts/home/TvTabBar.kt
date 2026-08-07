package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography

@Composable
fun TvTabBar(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSelectedTab: Boolean = true,
    onSelectedTabFocus: () -> Unit = {},
    focusSelectedTab: Boolean = false,
    onConsumeFocusRequest: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnSelectedTabFocus by rememberUpdatedState(onSelectedTabFocus)
    LaunchedEffect(autoFocusSelectedTab) {
        if (autoFocusSelectedTab) {
            runCatching { focusRequester.requestFocus() }
            currentOnSelectedTabFocus()
        }
    }
    val currentOnConsumeFocusRequest by rememberUpdatedState(onConsumeFocusRequest)
    LaunchedEffect(focusSelectedTab) {
        if (focusSelectedTab) {
            runCatching { focusRequester.requestFocus() }
            currentOnConsumeFocusRequest()
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.tvColors.backgroundSunken, RoundedCornerShape(percent = 50))
            .padding(3.dp),
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.focusRestorer(),
            containerColor = Color.Transparent,
            indicator = @Composable { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(selectedTabIndex)?.let { currentTabPosition ->
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = currentTabPosition,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.tvColors.backgroundActive,
                        inactiveColor = MaterialTheme.tvColors.backgroundActive,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedTabIndex,
                    onFocus = { onTabSelect(index) },
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 21.dp)
                        .then(if (index == selectedTabIndex) Modifier.focusRequester(focusRequester) else Modifier),
                    colors = TabDefaults.pillIndicatorTabColors(
                        contentColor = MaterialTheme.tvColors.textPrimary,
                        selectedContentColor = MaterialTheme.tvColors.textPrimaryActive,
                        focusedContentColor = MaterialTheme.tvColors.textPrimary,
                        focusedSelectedContentColor = MaterialTheme.tvColors.textPrimaryActive,
                        inactiveContentColor = MaterialTheme.tvColors.textPrimary,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (tab) {
                            is TvTab.TextTab -> {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    color = LocalContentColor.current,
                                    style = MaterialTheme.tvTypography.caption1,
                                )
                            }

                            is TvTab.IconTab -> {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = stringResource(tab.contentDescriptionRes),
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            is TvTab.TextWithIconTab -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        color = LocalContentColor.current,
                                        style = MaterialTheme.tvTypography.caption1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun TvTabBarPreview() {
    TvTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = selectedIndex,
            onTabSelect = { selectedIndex = it },
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun TvTabBarFirstSelectedPreview() {
    TvTheme {
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = 0,
            onTabSelect = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun TvTabBarSearchSelectedPreview() {
    TvTheme {
        TvTabBar(
            tabs = TvTab.entries,
            selectedTabIndex = 4,
            onTabSelect = {},
        )
    }
}
