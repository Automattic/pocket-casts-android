package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class CreateEmailPageState(
    val isCar: Boolean,
    val isLandscape: Boolean,
    val email: String,
    val password: String,
    val isEmailValid: Boolean,
    val isFormValid: Boolean,
    val isPasswordInvalid: Boolean,
    val isProgressVisible: Boolean,
    val errorText: String,
)

@Composable
internal fun CreateEmailPage(
    state: CreateEmailPageState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFocusChange: (isEmail: Boolean) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    val fields = @Composable {
        AccountFields(
            email = state.email,
            password = state.password,
            isEmailValid = state.isEmailValid,
            requestFocus = !state.isCar,
            spacing = if (state.isCar && !state.isLandscape) 24.dp else 16.dp,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onPasswordImeAction = { keyboardController?.hide() },
            emailPlaceholder = stringResource(if (state.isCar) LR.string.profile_email else LR.string.profile_email_address),
            emailImeAction = ImeAction.Done,
            isCreatingAccount = true,
            onEmailImeAction = { keyboardController?.hide() },
            onFocusChange = onFocusChange,
        )
    }
    val texts = @Composable { textAlign: TextAlign? ->
        TextP50(
            text = stringResource(LR.string.profile_create_password_requirements),
            color = if (state.isPasswordInvalid) colors.support05 else colors.primaryText02,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth().padding(top = if (state.isCar) 8.dp else 16.dp),
        )
        TextP50(
            text = state.errorText,
            color = colors.support05,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
    val nextButton = @Composable { buttonModifier: Modifier ->
        RowLoadingButton(
            text = stringResource(if (state.isCar) LR.string.profile_create_account else LR.string.profile_next),
            onClick = onNextClick,
            enabled = state.isFormValid,
            isLoading = state.isProgressVisible,
            includePadding = false,
            fontSize = if (state.isCar) 28.sp else 16.sp,
            modifier = buttonModifier,
        )
    }
    val background = modifier
        .fillMaxSize()
        .background(colors.primaryUi01)
    when {
        state.isCar -> BoxWithConstraints(modifier = background) {
            val viewportHeight = maxHeight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
            ) {
                TextH10(
                    text = stringResource(LR.string.profile_create_account),
                    fontSize = 56.sp,
                    lineHeight = 64.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = if (state.isLandscape) 16.dp else 72.dp),
                )
                val fieldsModifier = if (state.isLandscape) Modifier.padding(horizontal = 220.dp) else Modifier.widthIn(max = 500.dp)
                Column(modifier = fieldsModifier.padding(top = 16.dp)) {
                    fields()
                    texts(if (state.isLandscape) TextAlign.Center else null)
                }
                Spacer(Modifier.height(if (state.isLandscape) 30.dp else 16.dp))
                nextButton(Modifier.width(591.dp))
            }
        }

        state.isLandscape -> Column(modifier = background.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(32.dp))
                fields()
                texts(null)
            }
            nextButton(Modifier)
        }

        else -> Column(
            modifier = background
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            fields()
            texts(null)
            nextButton(Modifier.padding(top = 8.dp))
        }
    }
}
