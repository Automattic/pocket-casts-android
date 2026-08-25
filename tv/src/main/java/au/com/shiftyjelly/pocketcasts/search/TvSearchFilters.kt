package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
internal fun TvSearchFilters(
    selected: TvSearchFilter,
    onFilterFocus: (TvSearchFilter) -> Unit,
    onFilterClick: (TvSearchFilter) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<TvSearchFilter> = TvSearchFilter.entries,
    upFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterDivider(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(20.dp))
        TvSearchFilterPills(
            filters = filters,
            selected = selected,
            onFilterFocus = onFilterFocus,
            onFilterClick = onFilterClick,
            upFocusRequester = upFocusRequester,
        )
        Spacer(modifier = Modifier.width(20.dp))
        FilterDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TvSearchFilterPills(
    filters: List<TvSearchFilter>,
    selected: TvSearchFilter,
    onFilterFocus: (TvSearchFilter) -> Unit,
    onFilterClick: (TvSearchFilter) -> Unit,
    upFocusRequester: FocusRequester? = null,
) {
    val selectedIndex = filters.indexOf(selected)
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .background(MaterialTheme.tvColors.backgroundSunken, RoundedCornerShape(percent = 50))
            .padding(3.dp),
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.focusProperties {
                onEnter = { runCatching { focusRequester.requestFocus() } }
            },
            containerColor = Color.Transparent,
            indicator = @Composable { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(selectedIndex)?.let { currentTabPosition ->
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = currentTabPosition,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.tvColors.backgroundActive,
                        inactiveColor = MaterialTheme.tvColors.backgroundBase,
                    )
                }
            },
        ) {
            filters.forEachIndexed { index, filter ->
                Tab(
                    selected = index == selectedIndex,
                    onFocus = { onFilterFocus(filter) },
                    onClick = { onFilterClick(filter) },
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 21.dp)
                        .focusProperties { upFocusRequester?.let { up = it } }
                        .then(if (index == selectedIndex) Modifier.focusRequester(focusRequester) else Modifier),
                    colors = TabDefaults.pillIndicatorTabColors(
                        contentColor = MaterialTheme.tvColors.textPrimary,
                        selectedContentColor = MaterialTheme.tvColors.textPrimary,
                        focusedContentColor = MaterialTheme.tvColors.textPrimary,
                        focusedSelectedContentColor = MaterialTheme.tvColors.textPrimaryActive,
                        inactiveContentColor = MaterialTheme.tvColors.textPrimary,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(filter.labelRes),
                            color = LocalContentColor.current,
                            style = MaterialTheme.tvTypography.caption1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(MaterialTheme.tvColors.overlayBorder),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchFiltersPreview() {
    TvTheme {
        TvSearchFilters(
            selected = TvSearchFilter.Podcasts,
            onFilterFocus = {},
            onFilterClick = {},
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        )
    }
}
