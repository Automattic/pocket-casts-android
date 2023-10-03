package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailAndPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChangeEmailFragmentPage(
    onBackPressed: () -> Unit,
) {

    var enteredEmail by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_change_email_address_title),
            bottomShadow = true,
            onNavigationClick = onBackPressed
        )

        Row(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextH50(
                modifier = Modifier
                    .padding(end = 8.dp),
                text = stringResource(LR.string.profile_current_email),
                color = MaterialTheme.theme.colors.primaryText02,
            )
            TextH50(
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
                text = "stringResource(R.string.profile_current_email)",
                color = MaterialTheme.theme.colors.primaryText01,
            )
        }

        EmailAndPasswordFields(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp
                ),
            email = enteredEmail,
            emailPlaceholder = stringResource(LR.string.profile_new_email_address),
            password = enteredPassword,
            passwordPlaceholder = stringResource(LR.string.profile_confirm_password),
            showEmailError = false,
            showPasswordError = false,
            enabled = true,
            focusEnabled = false,
            onDone = {},
            onUpdateEmail = {
                enteredEmail = it
            },
            onUpdatePassword = {
                enteredPassword = it
            },
            showPasswordErrorMessage = false,
            isCreatingAccount = false,
        )

        RowLoadingButton(
            text = stringResource(LR.string.profile_confirm),
            onClick = {},
            isLoading = true,
            enabled = true,
        )
    }
}

@Preview
@Composable
private fun ChangeEmailFragmentPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        ChangeEmailFragmentPage(
            onBackPressed = {}
        )
    }
}
