package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun TvEmailSignInForm(
    state: TvEmailSignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val signingInDescription = stringResource(LR.string.tv_sign_in_signing_in)

    Column(
        modifier = modifier.width(FormWidth),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TvSignInTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = stringResource(LR.string.profile_email),
            enabled = !state.isSubmitting,
            errorText = if (state.showEmailError) stringResource(LR.string.onboarding_email_invalid_message) else null,
            contentType = ContentType.Username + ContentType.EmailAddress,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { runCatching { passwordFocusRequester.requestFocus() } }),
        )
        TvSignInTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(LR.string.profile_password),
            enabled = !state.isSubmitting,
            errorText = if (state.showPasswordError) {
                stringResource(LR.string.profile_create_password_requirements)
            } else {
                null
            },
            contentType = ContentType.Password,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onSubmit()
                },
            ),
            focusRequester = passwordFocusRequester,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            state.serverError?.let { serverError ->
                Text(
                    text = serverError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.tvTypography.caption1,
                )
            }
        }
        Button(
            onClick = onSubmit,
            colors = TvButtonDefaults.filledButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isSubmitting) {
                    LoadingView(
                        color = LocalContentColor.current,
                        modifier = Modifier
                            .size(24.dp)
                            .semantics { contentDescription = signingInDescription },
                    )
                } else {
                    Text(text = stringResource(LR.string.log_in))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvSignInTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    contentType: ContentType,
    enabled: Boolean = true,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardInsetPx = with(LocalDensity.current) { TvSignInKeyboardInset.toPx() }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var isFocused by remember { mutableStateOf(false) }
    val fieldColor = if (isFocused) Color.White else MaterialTheme.tvColors.backgroundActive20
    val contentColor = if (isFocused) MaterialTheme.tvColors.backgroundSunken else MaterialTheme.tvColors.textPrimary
    val placeholderColor = if (isFocused) contentColor.copy(alpha = 0.5f) else MaterialTheme.tvColors.textSecondary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.tvTypography.body.copy(color = contentColor),
            cursorBrush = SolidColor(contentColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { this.contentType = contentType }
                .onSizeChanged { fieldSize = it }
                .bringIntoViewRequester(bringIntoViewRequester)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) {
                        keyboardController?.show()
                    } else {
                        keyboardController?.hide()
                    }
                }
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        event.key == Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                        else -> false
                    }
                },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(fieldColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = placeholderColor,
                            style = MaterialTheme.tvTypography.body,
                        )
                    }
                    innerTextField()
                }
            },
        )
        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.tvTypography.caption1,
            )
        }
    }

    LaunchedEffect(isFocused) {
        if (!isFocused) return@LaunchedEffect
        delay(IME_SETTLE_MILLIS)
        bringIntoViewRequester.bringIntoView(
            Rect(0f, 0f, fieldSize.width.toFloat(), fieldSize.height + keyboardInsetPx),
        )
    }
}

private val FormWidth = 420.dp
private const val IME_SETTLE_MILLIS = 250L

/** Estimated height of the TV soft keyboard, which reports no window insets, used to lift a focused field clear of it. */
internal val TvSignInKeyboardInset = 320.dp
