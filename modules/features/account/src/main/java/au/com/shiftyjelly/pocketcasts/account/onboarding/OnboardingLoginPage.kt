package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
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

        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            emailFocusRequester.requestFocus()
        }

        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_welcome_back),
            onNavigationClick = onBackPressed
        )

        FormField(
            value = logInState.email,
            placeholder = stringResource(LR.string.profile_email_address),
            onValueChange = { viewModel.updateEmail(it) },
            enabled = logInState.enableTextFields,
            isError = logInState.hasError,
            leadingIcon = {
                if (logInState.hasError) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.theme.colors.support05
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                    )
                }
            },
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
            onNext = { passwordFocusRequester.requestFocus() },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .focusRequester(emailFocusRequester)
        )

        var showPassword by remember { mutableStateOf(false) }
        FormField(
            value = logInState.password,
            placeholder = stringResource(LR.string.profile_password),
            onValueChange = { viewModel.updatePassword(it) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
            capitalization = KeyboardCapitalization.None,
            enabled = logInState.enableTextFields,
            imeAction = ImeAction.Done,
            onNext = { viewModel.signIn() },
            isError = logInState.hasError,
            leadingIcon = {
                if (logInState.hasError) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_password),
                        contentDescription = null,
                        tint = MaterialTheme.theme.colors.support05
                    )
                } else {
                    Icon(
                        painter = painterResource(IR.drawable.ic_password),
                        contentDescription = null,
                    )
                }
            },
            trailingIcon = {
                val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(
                    onClick = { showPassword = !showPassword }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .focusRequester(passwordFocusRequester)
        )

        logInState.serverErrorMessage?.let {
            TextP40(
                text = it,
                color = MaterialTheme.theme.colors.support05,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        TextH40(
            text = stringResource(LR.string.onboarding_forgot_password),
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

@Preview
@Composable
fun OnBoardingLoginPage_Preview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT_CONTRAST) {
        OnboardingLoginPage(
            onBackPressed = {},
            onLoginComplete = {},
            onForgotPasswordTapped = {},
        )
    }
}
