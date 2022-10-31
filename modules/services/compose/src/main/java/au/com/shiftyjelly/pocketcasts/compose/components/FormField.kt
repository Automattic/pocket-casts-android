package au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components

import android.view.KeyEvent.ACTION_DOWN
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FormField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(if (singleLine) it.removeNewLines() else it) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.theme.colors.primaryText01,
            placeholderColor = MaterialTheme.theme.colors.primaryText02,
            unfocusedBorderColor = MaterialTheme.theme.colors.primaryField03,
        ),
        enabled = enabled,
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onNext() }),
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent {
                if (singleLine && it.key == Key.Enter && it.nativeKeyEvent.action == ACTION_DOWN) {
                    // the enter key for a single line field should call the next event, but for multiline fields it should be a new line.
                    onNext()
                    true
                } else if (it.key == Key.Tab && it.nativeKeyEvent.action == ACTION_DOWN) {
                    // tab should focus on the next field
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                } else {
                    false
                }
            }
    )
}

@Preview
@Composable
private fun FormFieldPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Box(Modifier.background(MaterialTheme.theme.colors.primaryUi03).padding(8.dp)) {
            FormField(
                value = "",
                placeholder = "Email",
                onValueChange = {}
            )
        }
    }
}
