package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLogInViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailAndPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginPage(
    onBackPressed: () -> Unit,
    onLoginComplete: () -> Unit,
    onForgotPasswordTapped: () -> Unit,
) {

    val viewModel = hiltViewModel<OnboardingLogInViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.onShown() }
    BackHandler {
        viewModel.onBackPressed()
        onBackPressed()
    }

    val view = LocalView.current
    @Suppress("NAME_SHADOWING")
    val onLoginComplete = {
        UiUtil.hideKeyboard(view)
        onLoginComplete()
    }

    Column {

        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_welcome_back),
            onNavigationClick = {
                viewModel.onBackPressed()
                onBackPressed()
            }
        )

        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {

            EmailAndPasswordFields(
                email = state.email,
                password = state.password,
                showEmailError = state.showEmailError,
                showPasswordError = state.showPasswordError,
                enabled = state.enableSubmissionFields,
                onDone = { viewModel.logIn(onLoginComplete) },
                onUpdateEmail = viewModel::updateEmail,
                onUpdatePassword = viewModel::updatePassword,
                isCreatingAccount = false,
                modifier = Modifier.padding(16.dp),
            )

            state.serverErrorMessage?.let {
                TextP40(
                    text = it,
                    color = MaterialTheme.theme.colors.support05,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(
                Modifier
                    .heightIn(min = 16.dp)
                    .weight(1f)
            )

            TextH40(
                text = stringResource(LR.string.onboarding_forgot_password),
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier
                    .clickable { onForgotPasswordTapped() }
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            RowButton(
                text = stringResource(LR.string.onboarding_log_in),
                enabled = state.enableSubmissionFields,
                onClick = { viewModel.logIn(onLoginComplete) },
            )
        }
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
