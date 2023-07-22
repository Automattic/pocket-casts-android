package au.com.shiftyjelly.pocketcasts.compose.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.onEnter
import au.com.shiftyjelly.pocketcasts.compose.extensions.onTabMoveFocus
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun FormField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = FormFieldDefaults.keyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(if (singleLine) it.removeNewLines() else it) },
        isError = isError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.theme.colors.primaryText01,
            placeholderColor = MaterialTheme.theme.colors.primaryText02,
            unfocusedBorderColor = if (isError) MaterialTheme.theme.colors.support05 else MaterialTheme.theme.colors.primaryField03,
            errorTrailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity) // Keep trailing icon the same color in error states
        ),
        enabled = enabled,
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions { onImeAction() },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .onTabMoveFocus()
            .let {
                if (singleLine) it.onEnter(onImeAction) else it
            }
    )
}

@ShowkaseComposable(name = "FormField", group = "Form", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun FormFieldLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FormFieldPreview()
    }
}

@ShowkaseComposable(name = "FormField", group = "Form", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun FormFieldDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        FormFieldPreview()
    }
}

@Composable
private fun FormFieldPreview() {
    Box(Modifier.background(MaterialTheme.theme.colors.primaryUi03).padding(8.dp)) {
        FormField(
            value = "",
            placeholder = "Email",
            onValueChange = {}
        )
    }
}
