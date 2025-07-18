package au.com.shiftyjelly.pocketcasts.settings.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.settings.rowModifier
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun HistoryPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onUpNextHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomInset),
    ) {
        item {
            ThemedTopAppBar(
                title = stringResource(LR.string.restore_from_local_history),
                onNavigationClick = onBackPress,
            )
        }
        item {
            UpNextHistoryRow(onClick = onUpNextHistoryClick)
        }
    }
}

@Composable
private fun UpNextHistoryRow(onClick: () -> Unit) {
    SettingRow(
        primaryText = stringResource(LR.string.up_next_history),
        icon = painterResource(IR.drawable.ic_upnext),
        modifier = Modifier.rowModifier(onClick),
    )
}
