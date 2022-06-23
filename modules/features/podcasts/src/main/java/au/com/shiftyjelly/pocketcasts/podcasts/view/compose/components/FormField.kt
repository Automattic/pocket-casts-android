package au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components

import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines

@Composable
fun FormField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.removeNewLines()) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.theme.colors.primaryText01,
            placeholderColor = MaterialTheme.theme.colors.primaryText02
        ),
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction, capitalization = KeyboardCapitalization.Sentences),
        keyboardActions = KeyboardActions(onAny = { onNext() }),
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent {
                if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    onNext()
                    true
                } else {
                    false
                }
            }
    )
}
