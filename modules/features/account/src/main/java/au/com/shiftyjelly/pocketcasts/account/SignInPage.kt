package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailField
import au.com.shiftyjelly.pocketcasts.compose.components.PasswordField
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class SignInPageState(
    val isCar: Boolean,
    val isLandscape: Boolean,
    val email: String,
    val password: String,
    val isEmailValid: Boolean,
    val isFormValid: Boolean,
    val isLoading: Boolean,
    val isProgressVisible: Boolean,
    val errorText: String?,
)

@Composable
internal fun SignInPage(
    state: SignInPageState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fields = @Composable {
        AccountFields(
            email = state.email,
            password = state.password,
            isEmailValid = state.isEmailValid,
            requestFocus = !state.isCar,
            spacing = when {
                state.isCar && !state.isLandscape -> 56.dp
                state.isLandscape -> 16.dp
                else -> 8.dp
            },
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onPasswordImeAction = onSignInClick,
        )
    }
    val errorText = @Composable {
        TextH40(text = state.errorText.orEmpty(), color = MaterialTheme.theme.colors.support05, modifier = Modifier.padding(top = 8.dp))
    }
    val forgotPassword = @Composable {
        RowTextButton(text = stringResource(LR.string.profile_forgot_your_password), onClick = onForgotPasswordClick, includePadding = false, fullWidth = false)
    }
    val signIn = @Composable { buttonModifier: Modifier ->
        RowLoadingButton(
            text = stringResource(LR.string.sign_in),
            onClick = onSignInClick,
            enabled = state.isFormValid && !state.isLoading,
            isLoading = state.isProgressVisible,
            includePadding = false,
            fontSize = if (state.isCar) 28.sp else 16.sp,
            modifier = buttonModifier,
        )
    }
    val background = modifier
        .fillMaxSize()
        .background(MaterialTheme.theme.colors.primaryUi01)
    when {
        state.isCar -> BoxWithConstraints(modifier = background) {
            val viewportHeight = maxHeight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (state.isLandscape) Arrangement.Top else Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
            ) {
                TextH10(
                    text = stringResource(LR.string.profile_sign_in),
                    fontSize = 56.sp,
                    lineHeight = 64.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = if (state.isLandscape) 0.dp else 44.dp),
                )
                Column(modifier = Modifier.padding(start = 220.dp, top = if (state.isLandscape) 16.dp else 96.dp, end = 220.dp)) {
                    fields()
                    if (state.errorText != null || !state.isLandscape) {
                        errorText()
                    }
                }
                Spacer(Modifier.height(if (state.isLandscape) 8.dp else 120.dp))
                forgotPassword()
                Spacer(Modifier.height(if (state.isLandscape) 32.dp else 54.dp))
                signIn(Modifier.width(591.dp))
            }
        }

        state.isLandscape -> Column(modifier = background.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                fields()
                errorText()
                Spacer(Modifier.height(32.dp))
                Column(Modifier.align(Alignment.CenterHorizontally)) { forgotPassword() }
            }
            signIn(Modifier)
        }

        else -> Column(
            modifier = background
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            fields()
            errorText()
            Spacer(Modifier.height(8.dp))
            Column(Modifier.align(Alignment.CenterHorizontally)) { forgotPassword() }
            Spacer(Modifier.height(8.dp))
            signIn(Modifier)
        }
    }
}

@Composable
internal fun AccountFields(
    email: String,
    password: String,
    isEmailValid: Boolean,
    requestFocus: Boolean,
    spacing: Dp,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    emailPlaceholder: String = stringResource(LR.string.profile_email),
    emailImeAction: ImeAction = ImeAction.Next,
    isCreatingAccount: Boolean = false,
    onEmailImeAction: (() -> Unit)? = null,
    onFocusChange: (isEmail: Boolean) -> Unit = {},
) {
    val passwordFocusRequester = remember { FocusRequester() }
    Column(modifier = modifier) {
        AccountEmailField(
            email = email,
            isEmailValid = isEmailValid,
            requestFocus = requestFocus,
            onEmailChange = onEmailChange,
            onImeAction = onEmailImeAction ?: {
                passwordFocusRequester.requestFocus()
                Unit
            },
            placeholder = emailPlaceholder,
            imeAction = emailImeAction,
            isCreatingAccount = isCreatingAccount,
            modifier = Modifier.onFocusChanged { onFocusChange(true) },
        )
        Spacer(Modifier.height(spacing))
        PasswordField(
            password = password,
            enabled = true,
            isError = false,
            isCreatingAccount = isCreatingAccount,
            onImeAction = onPasswordImeAction,
            onUpdatePassword = onPasswordChange,
            focusRequester = passwordFocusRequester,
            modifier = Modifier.onFocusChanged { onFocusChange(false) },
        )
    }
}

@Composable
internal fun AccountEmailField(
    email: String,
    isEmailValid: Boolean,
    requestFocus: Boolean,
    onEmailChange: (String) -> Unit,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(LR.string.profile_email),
    imeAction: ImeAction = ImeAction.Next,
    isCreatingAccount: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }
    EmailField(
        email = email,
        enabled = true,
        isError = false,
        isCreatingAccount = isCreatingAccount,
        onImeAction = onImeAction,
        onUpdateEmail = onEmailChange,
        placeholder = placeholder,
        imeAction = imeAction,
        focusRequester = focusRequester,
        trailingIcon = if (isEmailValid) {
            {
                Icon(
                    painter = painterResource(IR.drawable.ic_tick_circle),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.support02,
                )
            }
        } else {
            null
        },
        modifier = modifier,
    )
}
