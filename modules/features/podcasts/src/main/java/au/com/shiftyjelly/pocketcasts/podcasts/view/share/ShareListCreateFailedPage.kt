package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * The page shown when generating a sharing a list of podcasts fails.
 */
@Composable
fun ShareListCreateFailedPage(
    onCloseClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.podcasts_share_creating_list),
            navigationButton = NavigationButton.Close,
            onNavigationClick = onCloseClick
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_failedwarning),
                tint = MaterialTheme.theme.colors.primaryIcon01,
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(16.dp))
            TextH20(stringResource(LR.string.podcasts_share_failed))
            Spacer(Modifier.height(16.dp))
            TextP50(
                text = stringResource(LR.string.podcasts_share_failed_description),
                color = MaterialTheme.theme.colors.primaryText02
            )
            Spacer(Modifier.height(32.dp))
            TextButton(onClick = onRetryClick) {
                Text(stringResource(LR.string.retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareListCreateFailedPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        ShareListCreateFailedPage(
            onCloseClick = {},
            onRetryClick = {}
        )
    }
}
