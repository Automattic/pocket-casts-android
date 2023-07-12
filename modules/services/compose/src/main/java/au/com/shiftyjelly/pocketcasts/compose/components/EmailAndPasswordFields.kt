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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.airbnb.android.showkase.annotation.ShowkaseComposable
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
    showEmailError: Boolean,
    showPasswordError: Boolean,
    showPasswordErrorMessage: Boolean = showPasswordError,
    enabled: Boolean,
    isCreatingAccount: Boolean,
    focusEnabled: Boolean = true,
    onDone: () -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier
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
            enabled = enabled,
            imeAction = ImeAction.Done,
            onImeAction = onDone,
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
    isCreatingAccount: Boolean,
    modifier: Modifier = Modifier
) {

    @Suppress("NAME_SHADOWING")
    @OptIn(ExperimentalComposeUiApi::class)
    val modifier = modifier.focusRequester(focusRequester)
        .autofill(emailAutofill(isCreatingAccount), onUpdateEmail)

    FormField(
        value = email,
        placeholder = stringResource(LR.string.profile_email_address),
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
        modifier = modifier
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
    isCreatingAccount: Boolean,
    onUpdatePassword: (String) -> Unit,
    modifier: Modifier = Modifier

) {
    var showPassword by remember { mutableStateOf(false) }

    @OptIn(ExperimentalComposeUiApi::class)
    @Suppress("NAME_SHADOWING")
    val modifier = modifier.focusRequester(focusRequester)
        .autofill(passwordAutofill(isCreatingAccount), onUpdatePassword)

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
            Icon(
                painter = painterResource(IR.drawable.ic_password),
                contentDescription = null,
            )
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
        modifier = modifier
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun emailAutofill(isNewEmail: Boolean) =
    listOf(if (isNewEmail) AutofillType.NewUsername else AutofillType.Username)

@OptIn(ExperimentalComposeUiApi::class)
private fun passwordAutofill(isNewPassword: Boolean) =
    listOf(if (isNewPassword) AutofillType.NewPassword else AutofillType.Password)

/**
 * From https://bryanherbst.com/2021/04/13/compose-autofill/
 * Also see https://issuetracker.google.com/issues/176949051 for info about autofill limitations with compose
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this.onGloballyPositioned {
        autofillNode.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        autofill?.run {
            if (focusState.isFocused) {
                requestAutofillForNode(autofillNode)
            } else {
                cancelAutofillForNode(autofillNode)
            }
        }
    }
}

@ShowkaseComposable(name = "EmailAndPasswordFields", group = "Form", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun UserPasswordFieldsLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        UserPasswordFieldsPreview()
    }
}

@ShowkaseComposable(name = "EmailAndPasswordFields", group = "Form", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun UserPasswordFieldsDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        UserPasswordFieldsPreview()
    }
}

@Preview
@Composable
private fun UserPasswordFieldsPreview() {
    EmailAndPasswordFields(
        email = "",
        password = "",
        showEmailError = false,
        showPasswordError = false,
        enabled = true,
        focusEnabled = false,
        onDone = {},
        onUpdateEmail = {},
        onUpdatePassword = {},
        showPasswordErrorMessage = false,
        isCreatingAccount = false,
    )
}
