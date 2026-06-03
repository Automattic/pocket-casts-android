package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvTabBar(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .padding(1.dp),
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
                modifier = Modifier
                    .height(66.dp)
                    .padding(horizontal = 32.dp)
                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
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
                            color = LocalContentColor.current,
                            fontSize = 25.sp,
                            fontWeight = FontWeight(510),
                            lineHeight = 32.sp,
                            letterSpacing = 0.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is TvTab.IconTab -> {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = stringResource(tab.contentDescriptionRes),
                            modifier = Modifier
                                .size(36.dp),
                        )
                    }

                    is TvTab.TextWithIconTab -> {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextH40(
                            text = stringResource(tab.labelRes),
                            color = LocalContentColor.current,
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
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            var selectedIndex by remember { mutableIntStateOf(1) }
            TvTabBar(
                tabs = TvTab.entries,
                selectedTabIndex = selectedIndex,
                onTabSelect = { selectedIndex = it },
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabBarFirstSelectedPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvTabBar(
                tabs = TvTab.entries,
                selectedTabIndex = 0,
                onTabSelect = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabBarSearchSelectedPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvTabBar(
                tabs = TvTab.entries,
                selectedTabIndex = 4,
                onTabSelect = {},
            )
        }
    }
}
