package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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

data class EmailPasswordFieldsState(
    val email: String,
    val password: String,
    val enabled: Boolean,
    val hasError: Boolean,
    val onPasswordDone: () -> Unit,
    val onUpdateEmail: (String) -> Unit,
    val onUpdatePassword: (String) -> Unit,
)

@Composable
fun EmailPasswordFields(
    state: EmailPasswordFieldsState,
    modifier: Modifier = Modifier
) {

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    Column(modifier) {
        FormField(
            value = state.email,
            placeholder = stringResource(LR.string.profile_email_address),
            onValueChange = state.onUpdateEmail,
            enabled = state.enabled,
            isError = state.hasError,
            leadingIcon = {
                if (state.hasError) {
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
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email,
            ),
            onNext = { passwordFocusRequester.requestFocus() },
            modifier = Modifier.focusRequester(emailFocusRequester)
        )

        Spacer(Modifier.height(16.dp))

        var showPassword by remember { mutableStateOf(false) }
        FormField(
            value = state.password,
            placeholder = stringResource(LR.string.profile_password),
            onValueChange = state.onUpdatePassword,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = FormFieldDefaults.keyboardOptions.copy(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
            ),
            enabled = state.enabled,
            onNext = state.onPasswordDone,
            isError = state.hasError,
            leadingIcon = {
                if (state.hasError) {
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
            modifier = Modifier.focusRequester(passwordFocusRequester)
        )
    }
}

@Preview
@Composable
private fun UserPasswordFieldsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        EmailPasswordFields(
            EmailPasswordFieldsState(
                email = "",
                password = "",
                enabled = true,
                hasError = false,
                onPasswordDone = {},
                onUpdateEmail = {},
                onUpdatePassword = {},
            )
        )
    }
}
