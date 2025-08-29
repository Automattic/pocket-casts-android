package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * @property showEmailError whether to display the email field with an error state and the email error message.
 * @property showPasswordError whether to display the password field with an error state.
 * @property showPasswordErrorMessage whether the password error message should be shown along with the error.
 */
@Composable
fun EmailAndPasswordFields(
    email: String,
    password: String,
    enabled: Boolean,
    isCreatingAccount: Boolean,
    showEmailError: Boolean,
    showPasswordError: Boolean,
    onConfirm: () -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier,
    emailPlaceholder: String = stringResource(LR.string.profile_email_address),
    passwordPlaceholder: String = stringResource(LR.string.profile_password),
    showPasswordErrorMessage: Boolean = showPasswordError,
    focusEnabled: Boolean = true,
) {
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (focusEnabled) {
            emailFocusRequester.requestFocus()
        }
    }

    Column(modifier) {
        EmailField(
            email = email,
            isError = showEmailError,
            placeholder = emailPlaceholder,
            enabled = enabled,
            onUpdateEmail = onUpdateEmail,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocusRequester.requestFocus() },
            isCreatingAccount = isCreatingAccount,
            focusRequester = emailFocusRequester,
        )

        if (showEmailError) {
            ErrorText(LR.string.onboarding_email_invalid_message)
        }

        Spacer(Modifier.height(16.dp))

        PasswordField(
            password = password,
            isError = showPasswordError,
            placeholder = passwordPlaceholder,
            enabled = enabled,
            imeAction = ImeAction.Done,
            onImeAction = onConfirm,
            focusRequester = passwordFocusRequester,
            isCreatingAccount = isCreatingAccount,
            onUpdatePassword = onUpdatePassword,
        )

        if (showPasswordError && showPasswordErrorMessage) {
            ErrorText(LR.string.profile_create_password_requirements)
        }
    }
}

@Composable
private fun ErrorText(
    @StringRes textRes: Int,
) {
    TextP40(
        text = stringResource(textRes),
        color = MaterialTheme.theme.colors.support05,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun EmailField(
    email: String,
    enabled: Boolean,
    isError: Boolean,
    isCreatingAccount: Boolean,
    onImeAction: () -> Unit,
    onUpdateEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(LR.string.profile_email_address),
    imeAction: ImeAction = ImeAction.Next,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val formModifier = modifier.focusRequester(focusRequester)
        .semantics {
            contentType = (if (isCreatingAccount) ContentType.NewUsername else ContentType.Username) + ContentType.EmailAddress
        }

    FormField(
        value = email,
        placeholder = placeholder,
        onValueChange = onUpdateEmail,
        enabled = enabled,
        isError = isError,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
            )
        },
        keyboardOptions = FormFieldDefaults.keyboardOptions.copy(
            capitalization = KeyboardCapitalization.None,
            imeAction = imeAction,
            keyboardType = KeyboardType.Email,
        ),
        onImeAction = onImeAction,
        modifier = formModifier,
    )
}

@Composable
fun PasswordField(
    password: String,
    enabled: Boolean,
    isError: Boolean,
    isCreatingAccount: Boolean,
    onImeAction: () -> Unit,
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(LR.string.profile_password),
    imeAction: ImeAction = ImeAction.Done,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var showPassword by remember { mutableStateOf(false) }

    val formModifier = modifier.focusRequester(focusRequester)
        .semantics {
            contentType = if (isCreatingAccount) ContentType.NewPassword else ContentType.Password
        }

    FormField(
        value = password,
        placeholder = placeholder,
        onValueChange = onUpdatePassword,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = FormFieldDefaults.keyboardOptions.copy(
            capitalization = KeyboardCapitalization.None,
            imeAction = imeAction,
            keyboardType = KeyboardType.Password,
        ),
        enabled = enabled,
        onImeAction = onImeAction,
        isError = isError,
        leadingIcon = {
            Icon(
                painter = painterResource(IR.drawable.ic_password),
                contentDescription = null,
            )
        },
        trailingIcon = {
            val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
            IconButton(
                onClick = { showPassword = !showPassword },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        modifier = formModifier,
    )
}

@Preview(name = "Light")
@Composable
private fun UserPasswordFieldsLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        UserPasswordFieldsPreview()
    }
}

@Preview(name = "Dark")
@Composable
private fun UserPasswordFieldsDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        UserPasswordFieldsPreview()
    }
}

@Composable
private fun UserPasswordFieldsPreview() {
    EmailAndPasswordFields(
        email = "",
        password = "",
        showEmailError = false,
        showPasswordError = false,
        enabled = true,
        focusEnabled = false,
        onConfirm = {},
        onUpdateEmail = {},
        onUpdatePassword = {},
        showPasswordErrorMessage = false,
        isCreatingAccount = false,
        modifier = Modifier.padding(16.dp),
    )
}
