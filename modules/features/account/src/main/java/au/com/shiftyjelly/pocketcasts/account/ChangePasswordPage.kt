package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePasswordError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePasswordState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePwdViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.PasswordField
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChangePasswordPage(
    modifier: Modifier = Modifier,
    changePassword: () -> Unit,
    onSuccess: () -> Unit,
    onBackPressed: () -> Unit,
    bottomOffset: Dp,
    viewModel: ChangePwdViewModel,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val serverErrorMessage = stringResource(LR.string.server_login_password_change_failed)

    val state by viewModel.state.collectAsStateWithLifecycle()

    var invalidPasswordCurrent by remember { mutableStateOf(false) }
    var invalidPasswordNew by remember { mutableStateOf(false) }
    var invalidPasswordConfirm by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }

    val passwordCurrentFocusRequester = remember { FocusRequester() }
    val passwordNewFocusRequester = remember { FocusRequester() }
    val passwordConfirmFocusRequester = remember { FocusRequester() }

    val passwordCurrent: String by viewModel.passwordCurrent.observeAsState("")
    val passwordNew: String by viewModel.passwordNew.observeAsState("")
    val passwordConfirm: String by viewModel.passwordConfirm.observeAsState("")

    val onFormSubmit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        changePassword()
    }

    LaunchedEffect(state) {
        when (state) {
            is ChangePasswordState.Failure -> {
                val failureState = state as ChangePasswordState.Failure
                invalidPasswordCurrent = failureState.errors.contains(ChangePasswordError.INVALID_PASSWORD_CURRENT)
                invalidPasswordNew = failureState.errors.contains(ChangePasswordError.INVALID_PASSWORD_NEW)
                invalidPasswordConfirm = failureState.errors.contains(ChangePasswordError.INVALID_PASSWORD_CONFIRM)

                if (failureState.errors.contains(ChangePasswordError.SERVER)) {
                    errorMessage = serverErrorMessage
                    viewModel.clearServerError()
                }
            }

            is ChangePasswordState.Loading -> {
                errorMessage = ""
            }

            is ChangePasswordState.Success -> {
                onSuccess()
            }

            is ChangePasswordState.Empty -> {
                invalidPasswordCurrent = false
                invalidPasswordNew = false
                invalidPasswordConfirm = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_change_password_title),
            bottomShadow = true,
            onNavigationClick = onBackPressed,
        )

        LazyColumn(
            modifier = modifier
                .weight(1f, false)
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = bottomOffset),
        ) {
            item {
                PasswordField(
                    password = passwordCurrent,
                    isError = false,
                    placeholder = stringResource(LR.string.profile_current_password),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    onImeAction = { passwordNewFocusRequester.requestFocus() },
                    isCreatingAccount = false,
                    focusRequester = passwordCurrentFocusRequester,
                    onUpdatePassword = { viewModel.updatePwdCurrent(it) },
                )
            }
            item {
                PasswordField(
                    password = passwordNew,
                    isError = false,
                    placeholder = stringResource(LR.string.profile_new_password),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    onImeAction = { passwordConfirmFocusRequester.requestFocus() },
                    isCreatingAccount = false,
                    focusRequester = passwordNewFocusRequester,
                    onUpdatePassword = { viewModel.updatePwdNew(it) },
                )
            }
            item {
                Column {
                    PasswordField(
                        password = passwordConfirm,
                        isError = false,
                        placeholder = stringResource(LR.string.profile_confirm_new_password),
                        enabled = true,
                        imeAction = ImeAction.Done,
                        onImeAction = { keyboardController?.hide() },
                        isCreatingAccount = false,
                        focusRequester = passwordConfirmFocusRequester,
                        onUpdatePassword = { viewModel.updatePwdConfirm(it) },
                    )
                    TextH40(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = errorMessage,
                        color = MaterialTheme.theme.colors.support05,
                    )
                }
            }
            item {
                RowLoadingButton(
                    text = stringResource(LR.string.profile_confirm),
                    onClick = onFormSubmit,
                    isLoading = state is ChangePasswordState.Loading,
                    enabled = !invalidPasswordCurrent && !invalidPasswordNew && !invalidPasswordConfirm,
                    includePadding = false,
                )
            }
        }
    }
}
