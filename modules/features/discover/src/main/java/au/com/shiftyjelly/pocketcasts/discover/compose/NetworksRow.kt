package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun NetworksRow(
    networks: List<DiscoverListSummary>,
    onClickNetwork: (DiscoverListSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HorizontalMargin),
        horizontalArrangement = Arrangement.spacedBy(Gutter),
        modifier = modifier,
    ) {
        items(networks, key = { it.uuid }) { network ->
            NetworkCard(
                network = network,
                onClick = { onClickNetwork(network) },
                modifier = Modifier.width(NetworkCardWidth),
            )
        }
    }
}

private val HorizontalMargin = 16.dp
private val Gutter = 16.dp

@Preview(widthDp = 402)
@Composable
private fun NetworksRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NetworksRow(
            networks = List(4) { index -> NetworkPreview.copy(uuid = "network-$index") },
            onClickNetwork = {},
        )
    }
}
