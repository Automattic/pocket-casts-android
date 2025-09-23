package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ContinueWithGoogleButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingCreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.custom
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailAndPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun NewOnboardingCreateAccountPage(
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    onBackPress: () -> Unit,
    onSkip: () -> Unit,
    onCreateAccount: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    onContinueWithGoogleComplete: (GoogleSignInState, Subscription?) -> Unit,
    onClickLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingCreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()

    val pocketCastsTheme = MaterialTheme.theme

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

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(bottom = 16.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        if (flow.shouldOfferLogin) {
            NavigationIconButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                tint = MaterialTheme.theme.colors.primaryInteractive01,
                navigationButton = NavigationButton.Close,
                onClick = onSkip,
            )
        } else {
            TextP40(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, top = 11.dp)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                text = stringResource(LR.string.not_now),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                fontWeight = FontWeight.W500,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextH10(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(LR.string.onboarding_create_account_title),
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextP40(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(LR.string.onboarding_create_account_message),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            contentDescription = null,
            painter = painterResource(IR.drawable.artwork_create_account),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Spacer(modifier = Modifier.weight(2f))
        if (viewModel.showGoogleSignUp) {
            ContinueWithGoogleButton(
                flow = flow,
                onComplete = onContinueWithGoogleComplete,
                label = stringResource(
                    if (flow.shouldOfferLogin) {
                        LR.string.onboarding_continue_with_google
                    } else {
                        LR.string.onboarding_create_account_sign_up_google
                    },
                ),
            )
        }
        RowButton(
            text = stringResource(LR.string.onboarding_create_account_sign_up_email),
            enabled = state.enableSubmissionFields,
            onClick = onCreateAccount,
            includePadding = false,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (flow.shouldOfferLogin) {
            RowTextButton(
                text = stringResource(LR.string.onboarding_log_in),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
                includePadding = false,
                onClick = onClickLogin,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private val OnboardingFlow.shouldOfferLogin: Boolean
    get() = this is OnboardingFlow.LoggedOut || this is OnboardingFlow.Upsell || this is OnboardingFlow.UpsellSuggestedFolder

@Composable
internal fun OnboardingCreateAccountPage(
    theme: Theme.ThemeType,
    onBackPress: () -> Unit,
    onCreateAccount: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingCreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()

    val pocketCastsTheme = MaterialTheme.theme

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
    val onAccountCreated = {
        UiUtil.hideKeyboard(view)
        onCreateAccount()
    }

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.create_account),
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
                showPasswordErrorMessage = false,
                enabled = state.enableSubmissionFields,
                onConfirm = { viewModel.createAccount(onAccountCreated) },
                onUpdateEmail = viewModel::updateEmail,
                onUpdatePassword = viewModel::updatePassword,
                isCreatingAccount = true,
                modifier = Modifier.padding(16.dp),
            )

            TextP40(
                text = "• ${stringResource(LR.string.profile_create_password_requirements)}",
                color = if (state.showPasswordError) {
                    MaterialTheme.theme.colors.support05
                } else {
                    MaterialTheme.theme.colors.primaryText02
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            )

            state.errorMessage?.let { errorMessage ->
                TextP40(
                    text = errorMessage,
                    color = MaterialTheme.theme.colors.support05,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            RowButton(
                text = stringResource(LR.string.create_account),
                enabled = state.enableSubmissionFields,
                onClick = { viewModel.createAccount(onAccountCreated) },
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingCreateAccountPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingCreateAccountPage(
            theme = themeType,
            onBackPress = {},
            onCreateAccount = {},
            onUpdateSystemBars = {},
        )
    }
}
