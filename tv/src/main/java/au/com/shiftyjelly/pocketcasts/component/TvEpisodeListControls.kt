package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvArchivedFilterButton(
    isShowingArchived: Boolean,
    onToggleArchiveFilter: () -> Unit,
    modifier: Modifier = Modifier,
    leftFocusRequester: FocusRequester? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { isExpanded = true },
            colors = TvButtonDefaults.filledButtonColors(),
            modifier = if (leftFocusRequester != null) {
                Modifier.focusProperties { left = leftFocusRequester }
            } else {
                Modifier
            },
        ) {
            Text(
                text = stringResource(if (isShowingArchived) LR.string.show_archived else LR.string.podcast_hide_archived),
                style = MaterialTheme.tvTypography.caption2,
            )
            Icon(
                painter = painterResource(IR.drawable.ic_chevron_small_up),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(16.dp)
                    .rotate(180f),
            )
        }
        if (isExpanded) {
            TvDropdownMenu(onDismissRequest = { isExpanded = false }) {
                TvDropdownMenuItem(
                    label = stringResource(LR.string.podcast_hide_archived),
                    isSelected = !isShowingArchived,
                    onClick = {
                        isExpanded = false
                        if (isShowingArchived) {
                            onToggleArchiveFilter()
                        }
                    },
                )
                TvDropdownMenuItem(
                    label = stringResource(LR.string.show_archived),
                    isSelected = isShowingArchived,
                    onClick = {
                        isExpanded = false
                        if (!isShowingArchived) {
                            onToggleArchiveFilter()
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun <T> TvSortButton(
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                onExpand()
                isExpanded = true
            },
            colors = TvButtonDefaults.iconButtonColors(),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_sort),
                contentDescription = stringResource(LR.string.sort_by),
                modifier = Modifier.size(20.dp),
            )
        }
        if (isExpanded) {
            TvDropdownMenu(
                title = stringResource(LR.string.sort_by),
                onDismissRequest = { isExpanded = false },
            ) {
                options.forEach { option ->
                    TvDropdownMenuItem(
                        label = label(option),
                        isSelected = option == selected,
                        onClick = {
                            isExpanded = false
                            if (option != selected) {
                                onSelect(option)
                            }
                        },
                    )
                }
            }
        }
    }
}
