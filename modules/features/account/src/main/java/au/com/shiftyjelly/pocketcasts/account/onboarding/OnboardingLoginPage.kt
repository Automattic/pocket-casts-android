package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ContinueWithGoogleButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLogInViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.custom
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailAndPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun NewOnboardingLoginPage(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    onBackPress: () -> Unit,
    onLoginComplete: (Subscription?) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    onContinueWithGoogleComplete: (GoogleSignInState, Subscription?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingLogInViewModel = hiltViewModel(),
) {
    val pocketCastsTheme = MaterialTheme.theme
    val state by viewModel.state.collectAsState()

    CallOnce {
        viewModel.onShown()
    }

    LaunchedEffect(onUpdateSystemBars) {
        // Use secondaryUI01 so the status bar matches the ThemedTopAppBar
        val statusBar = SystemBarStyle.custom(pocketCastsTheme.colors.secondaryUi01, theme.toolbarLightIcons)
        val navigationBar = SystemBarStyle.transparent { theme.darkTheme }
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }
    BackHandler {
        viewModel.onBackPressed()
        onBackPress()
    }

    val view = LocalView.current

    @Suppress("NAME_SHADOWING")
    val onLoginComplete = { subscription: Subscription? ->
        UiUtil.hideKeyboard(view)
        onLoginComplete(subscription)
    }

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_log_in),
            onNavigationClick = {
                viewModel.onBackPressed()
                onBackPress()
            },
        )

        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            EmailAndPasswordFields(
                email = state.email,
                password = state.password,
                showEmailError = state.showEmailError,
                showPasswordError = state.showPasswordError,
                enabled = state.enableSubmissionFields,
                onConfirm = { viewModel.logIn(onLoginComplete) },
                onUpdateEmail = viewModel::updateEmail,
                onUpdatePassword = viewModel::updatePassword,
                isCreatingAccount = false,
                modifier = Modifier.padding(vertical = 16.dp),
                focusEnabled = false,
            )

            state.errorMessage?.let { errorMessage ->
                TextP40(
                    text = errorMessage,
                    color = MaterialTheme.theme.colors.support05,
                )
            }

            TextP50(
                text = stringResource(LR.string.profile_forgot_your_password),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                fontWeight = FontWeight.W500,
                modifier = Modifier
                    .clickable {
                        viewModel.onForgotPasswordTapped(flow)
                        onForgotPasswordClick()
                    },
            )

            Spacer(modifier = Modifier.height(16.dp))

            RowButton(
                text = stringResource(LR.string.onboarding_login_continue_with_email),
                enabled = state.enableSubmissionFields,
                onClick = {
                    viewModel.onSignInButtonTapped(flow)
                    viewModel.logIn(onLoginComplete)
                },
                includePadding = false,
            )
            if (viewModel.showContinueWithGoogleButton && !(flow is OnboardingFlow.Upsell || flow is OnboardingFlow.UpsellSuggestedFolder)) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(.75.dp)
                            .background(color = MaterialTheme.theme.colors.primaryUi05),
                    )
                    TextC50(
                        text = stringResource(LR.string.onboarding_login_or),
                        color = MaterialTheme.theme.colors.primaryText01,
                        fontWeight = FontWeight.W400,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(.75.dp)
                            .background(color = MaterialTheme.theme.colors.primaryUi05),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ContinueWithGoogleButton(
                    includePadding = false,
                    flow = flow,
                    onComplete = onContinueWithGoogleComplete,
                    event = AnalyticsEvent.SIGNIN_BUTTON_TAPPED,
                )
            }
        }
    }
}

@Composable
internal fun OnboardingLoginPage(
    theme: Theme.ThemeType,
    onBackPress: () -> Unit,
    onLoginComplete: (Subscription?) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingLogInViewModel = hiltViewModel(),
) {
    val pocketCastsTheme = MaterialTheme.theme
    val state by viewModel.state.collectAsState()

    CallOnce {
        viewModel.onShown()
    }

    LaunchedEffect(onUpdateSystemBars) {
        // Use secondaryUI01 so the status bar matches the ThemedTopAppBar
        val statusBar = SystemBarStyle.custom(pocketCastsTheme.colors.secondaryUi01, theme.toolbarLightIcons)
        val navigationBar = SystemBarStyle.transparent { theme.darkTheme }
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }
    BackHandler {
        viewModel.onBackPressed()
        onBackPress()
    }

    val view = LocalView.current

    @Suppress("NAME_SHADOWING")
    val onLoginComplete = { subscription: Subscription? ->
        UiUtil.hideKeyboard(view)
        onLoginComplete(subscription)
    }

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_welcome_back),
            onNavigationClick = {
                viewModel.onBackPressed()
                onBackPress()
            },
        )

        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            EmailAndPasswordFields(
                email = state.email,
                password = state.password,
                showEmailError = state.showEmailError,
                showPasswordError = state.showPasswordError,
                enabled = state.enableSubmissionFields,
                onConfirm = { viewModel.logIn(onLoginComplete) },
                onUpdateEmail = viewModel::updateEmail,
                onUpdatePassword = viewModel::updatePassword,
                isCreatingAccount = false,
                modifier = Modifier.padding(16.dp),
            )

            state.errorMessage?.let { errorMessage ->
                TextP40(
                    text = errorMessage,
                    color = MaterialTheme.theme.colors.support05,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(
                Modifier
                    .heightIn(min = 16.dp)
                    .weight(1f),
            )

            TextH40(
                text = stringResource(LR.string.onboarding_forgot_password),
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier
                    .clickable { onForgotPasswordClick() }
                    .align(Alignment.CenterHorizontally),
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
private fun OnboardingLoginPage_Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingLoginPage(
            theme = themeType,
            onBackPress = {},
            onLoginComplete = {},
            onForgotPasswordClick = {},
            onUpdateSystemBars = {},
        )
    }
}
