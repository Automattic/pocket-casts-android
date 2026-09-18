package au.com.shiftyjelly.pocketcasts.profile.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShareProfileNamePage(
    state: ShareProfileViewModel.State,
    onBackPress: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShareProfileNamePage(
        displayName = state.displayName,
        onDisplayNameChange = onDisplayNameChange,
        onBackPress = onBackPress,
        onContinueClick = onContinueClick,
        modifier = modifier,
    )
}

@Composable
private fun ShareProfileNamePage(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onBackPress: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trimmedName = displayName.trim()

    Column(modifier = modifier.fillMaxSize()) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_sharing_title),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onBackPress,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            TextH40(
                text = stringResource(LR.string.profile_sharing_display_name_label),
                color = MaterialTheme.theme.colors.primaryText01,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = displayName,
                placeholder = stringResource(LR.string.profile_sharing_display_name_placeholder),
                onValueChange = onDisplayNameChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onImeAction = {
                    if (trimmedName.isNotEmpty()) onContinueClick()
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextH50(
                text = stringResource(LR.string.profile_sharing_display_name_helper),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W400,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        RowButton(
            text = stringResource(LR.string.navigation_continue),
            enabled = trimmedName.isNotEmpty(),
            onClick = { onContinueClick() },
        )
    }
}

@Preview
@Composable
private fun ShareProfileNamePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = MaterialTheme.colors.background) {
            ShareProfileNamePage(
                displayName = "Dom",
                onDisplayNameChange = {},
                onBackPress = {},
                onContinueClick = {},
            )
        }
    }
}
