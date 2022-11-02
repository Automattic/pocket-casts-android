package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.LogInViewModel.LogInState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginPage(
    onBackPressed: () -> Unit,
    onLoginComplete: () -> Unit,
    onForgotPasswordTapped: () -> Unit,
) {
    val viewModel = hiltViewModel<LogInViewModel>()
    val logInState by viewModel.logInState.collectAsState()

    if (logInState.callState == LogInState.CallState.Successful) {
        @OptIn(ExperimentalComposeUiApi::class)
        LocalSoftwareKeyboardController.current?.hide()
        onLoginComplete()
        return
    }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {

        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_welcome_back),
            onNavigationClick = onBackPressed
        )

        EmailPasswordFields(
            state = logInState.emailPasswordState,
            modifier = Modifier.padding(16.dp)
        )

        logInState.errorMessage?.let {
            TextP40(
                text = it,
                color = MaterialTheme.theme.colors.support05,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        TextH40(
            text = stringResource(LR.string.onboarding_forgot_password),
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier
                .clickable { onForgotPasswordTapped() }
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        RowButton(
            text = stringResource(LR.string.log_in),
            enabled = logInState.enableLoginButton,
            onClick = { viewModel.signIn() },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnBoardingLoginPage_Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingLoginPage(
            onBackPressed = {},
            onLoginComplete = {},
            onForgotPasswordTapped = {},
        )
    }
}
