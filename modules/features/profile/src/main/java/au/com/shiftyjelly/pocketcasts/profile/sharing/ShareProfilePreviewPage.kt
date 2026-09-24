package au.com.shiftyjelly.pocketcasts.profile.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShareProfilePreviewPage(
    state: ShareProfileViewModel.State,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShareProfilePreviewPage(
        displayName = state.displayName,
        onBackPress = onBackPress,
        modifier = modifier,
    )
}

@Composable
private fun ShareProfilePreviewPage(
    displayName: String,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_sharing_preview_title),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onBackPress,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TextH20(
                text = displayName,
                color = MaterialTheme.theme.colors.primaryText01,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun ShareProfilePreviewPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = MaterialTheme.colors.background) {
            ShareProfilePreviewPage(
                displayName = "Dom",
                onBackPress = {},
            )
        }
    }
}
