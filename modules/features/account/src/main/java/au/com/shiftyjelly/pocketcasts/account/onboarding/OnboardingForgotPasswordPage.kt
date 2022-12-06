package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingForgotPasswordViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailField
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingForgotPasswordPage(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
    onCompleted: () -> Unit,
) {
    val emailFocusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val view = LocalView.current
    val onSuccess = {
        UiUtil.hideKeyboard(view)
        UiUtil.displayAlert(
            context,
            context.getString(LR.string.profile_reset_password_sent),
            context.getString(LR.string.profile_reset_password_check_email),
            onComplete = onCompleted
        )
    }

    LaunchedEffect(Unit) {
        onShown()
        emailFocusRequester.requestFocus()
    }
    BackHandler { onBackPressed() }

    val viewModel = hiltViewModel<OnboardingForgotPasswordViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_reset_password),
            onNavigationClick = onBackPressed
        )

        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {

            EmailField(
                email = state.email,
                enabled = state.enableSubmissionFields,
                isError = state.serverErrorMessage != null,
                onUpdateEmail = viewModel::updateEmail,
                imeAction = ImeAction.Done,
                onImeAction = { viewModel.resetPassword(onSuccess) },
                isCreatingAccount = false,
                modifier = Modifier
                    .focusRequester(emailFocusRequester)
                    .padding(16.dp),
            )

            state.serverErrorMessage?.let {
                TextP40(
                    text = it,
                    color = MaterialTheme.theme.colors.support05,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            RowButton(
                text = stringResource(LR.string.profile_confirm),
                enabled = state.enableSubmissionFields,
                onClick = { viewModel.resetPassword(onSuccess) },
            )
        }
    }
}

@Preview
@Composable
fun OnboardingForgotPasswordPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingForgotPasswordPage(
            onShown = {},
            onBackPressed = {},
            onCompleted = {},
        )
    }
}
