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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EmailAndPasswordFields(
    email: String,
    password: String,
    showEmailError: Boolean,
    showPasswordError: Boolean,
    enabled: Boolean,
    onDone: () -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    Column(modifier) {

        EmailField(
            email = email,
            isError = showEmailError,
            enabled = enabled,
            onUpdateEmail = onUpdateEmail,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocusRequester.requestFocus() },
            focusRequester = emailFocusRequester,
        )

        if (showEmailError) {
            ErrorText(LR.string.onboarding_email_invalid_message)
        }

        Spacer(Modifier.height(16.dp))

        PasswordField(
            password = password,
            isError = showPasswordError,
            enabled = enabled,
            imeAction = ImeAction.Done,
            onImeAction = onDone,
            focusRequester = passwordFocusRequester,
            onUpdatePassword = onUpdatePassword,
        )

        if (showPasswordError) {
            ErrorText(LR.string.onboarding_password_invalid_message)
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
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun EmailField(
    email: String,
    enabled: Boolean,
    isError: Boolean,
    onUpdateEmail: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    FormField(
        value = email,
        placeholder = stringResource(LR.string.profile_email_address),
        onValueChange = onUpdateEmail,
        enabled = enabled,
        isError = isError,
        leadingIcon = {
            if (isError) {
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
        keyboardOptions = FormFieldDefaults.keyboardOptions.copy(
            capitalization = KeyboardCapitalization.None,
            imeAction = imeAction,
            keyboardType = KeyboardType.Email,
        ),
        onImeAction = onImeAction,
        modifier = modifier.focusRequester(focusRequester)
    )
}

@Composable
fun PasswordField(
    password: String,
    enabled: Boolean,
    isError: Boolean,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier

) {
    var showPassword by remember { mutableStateOf(false) }

    FormField(
        value = password,
        placeholder = stringResource(LR.string.profile_password),
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
            if (isError) {
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
        modifier = modifier.focusRequester(focusRequester)
    )
}

@Preview
@Composable
private fun UserPasswordFieldsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        EmailAndPasswordFields(
            email = "",
            password = "",
            showEmailError = false,
            showPasswordError = false,
            enabled = true,
            onDone = {},
            onUpdateEmail = {},
            onUpdatePassword = {},
        )
    }
}
