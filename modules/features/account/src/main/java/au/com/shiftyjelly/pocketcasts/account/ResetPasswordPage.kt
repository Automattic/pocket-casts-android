package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class ResetPasswordPageState(
    val isCar: Boolean,
    val email: String,
    val isEmailValid: Boolean,
    val isProgressVisible: Boolean,
    val errorText: String,
)

@Composable
internal fun ResetPasswordPage(
    state: ResetPasswordPageState,
    onEmailChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val containerModifier = if (state.isCar) {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp)
    } else {
        modifier.padding(horizontal = 16.dp)
    }
    Column(modifier = containerModifier) {
        Spacer(Modifier.height(if (state.isCar) 0.dp else 32.dp))
        AccountEmailField(
            email = state.email,
            isEmailValid = state.isEmailValid,
            requestFocus = !state.isCar,
            onEmailChange = onEmailChange,
            onImeAction = { keyboardController?.hide() },
            imeAction = ImeAction.Done,
        )
        TextH40(text = state.errorText, color = MaterialTheme.theme.colors.support05, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(if (state.isCar) 8.dp else 32.dp))
        RowLoadingButton(
            text = stringResource(LR.string.profile_confirm),
            onClick = onConfirmClick,
            enabled = state.isEmailValid,
            isLoading = state.isProgressVisible,
            includePadding = false,
            fontSize = if (state.isCar) 28.sp else 16.sp,
        )
    }
}
