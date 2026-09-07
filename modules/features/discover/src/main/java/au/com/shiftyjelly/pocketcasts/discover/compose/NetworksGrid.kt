package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.NetworksGridViewModel.UiState
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun NetworksGrid(
    state: UiState,
    onClickNetwork: (DiscoverListSummary) -> Unit,
    onClickRetry: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(color = MaterialTheme.theme.colors.primaryIcon02)
        }

        is UiState.Error -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize(),
        ) {
            NetworksGridError(onClickRetry = onClickRetry)
        }

        is UiState.Loaded -> LazyVerticalGrid(
            // sizing the columns off the card keeps landscape and tablets near the designed 168dp rather than stretching two across
            columns = GridCells.Adaptive(NetworkCardWidth),
            contentPadding = PaddingValues(
                start = HorizontalMargin,
                end = HorizontalMargin,
                top = VerticalMargin,
                bottom = VerticalMargin + bottomInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(Gutter),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
            modifier = modifier.fillMaxSize(),
        ) {
            items(state.networks, key = { it.uuid }) { network ->
                NetworkCard(
                    network = network,
                    onClick = { onClickNetwork(network) },
                )
            }
        }
    }
}

@Composable
private fun NetworksGridError(
    onClickRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = HorizontalMargin),
    ) {
        TextP40(
            text = stringResource(LR.string.discover_error),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
        )
        RowOutlinedButton(
            text = stringResource(LR.string.discover_retry),
            onClick = onClickRetry,
        )
    }
}

private val HorizontalMargin = 24.dp
private val VerticalMargin = 16.dp
private val Gutter = 18.dp
private val RowSpacing = 26.dp

@Preview(name = "Landscape", widthDp = 874, heightDp = 402)
@Composable
private fun NetworksGridLandscapePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        NetworksGrid(
            state = UiState.Loaded(List(6) { index -> NetworkPreview.copy(uuid = "network-$index") }),
            onClickNetwork = {},
            onClickRetry = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(widthDp = 402)
@Composable
private fun NetworksGridPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NetworksGrid(
            state = UiState.Loaded(List(4) { index -> NetworkPreview.copy(uuid = "network-$index") }),
            onClickNetwork = {},
            onClickRetry = {},
            bottomInset = 0.dp,
        )
    }
}
