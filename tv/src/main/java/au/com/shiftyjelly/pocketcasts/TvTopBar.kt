package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvTopBar(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_launcher_foreground),
            contentDescription = stringResource(LR.string.app_name),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        TvTabBar(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = onTabSelect,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onProfileClick,
            colors = IconButtonDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_profile),
                contentDescription = stringResource(LR.string.profile),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTopBarPreview() {
    MaterialTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        TvTopBar(
            tabs = TvTab.entries,
            selectedTabIndex = selectedIndex,
            onTabSelect = { selectedIndex = it },
            onProfileClick = {},
        )
    }
}
