package au.com.shiftyjelly.pocketcasts.compose.components

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchBar(
    text: String,
    onTextChanged: (String) -> Unit,
    placeholder: String = stringResource(LR.string.search_podcasts_or_add_url),
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = {
            onTextChanged(it.removeNewLines())
        },
        placeholder = { Text(placeholder) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            cursorColor = MaterialTheme.theme.colors.primaryText02,
            textColor = MaterialTheme.theme.colors.primaryText01,
            placeholderColor = MaterialTheme.theme.colors.primaryText02,
            leadingIconColor = MaterialTheme.theme.colors.primaryIcon02,
            trailingIconColor = MaterialTheme.theme.colors.primaryIcon02,
            backgroundColor = MaterialTheme.theme.colors.primaryField01,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch()
                focusManager.clearFocus()
            }
        ),
        maxLines = 1,
        leadingIcon = {
            Icon(
                painter = painterResource(IR.drawable.ic_search),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onTextChanged("")
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_cancel),
                        contentDescription = stringResource(LR.string.cancel)
                    )
                }
            }
        },
        modifier = modifier.onKeyEvent {
            // close the keyboard on enter
            if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                focusManager.clearFocus()
                true
            } else {
                false
            }
        }
    )
}

@Composable
fun SearchBarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = text
                onClick {
                    onClick()
                    true
                }
            }
    ) {
        SearchBar(
            text = "",
            placeholder = text,
            onTextChanged = {},
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            Modifier
                .matchParentSize() // cover SearchBar
                .clickable { onClick() } // handle click events
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchBarLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        SearchBarPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchBarDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        SearchBarPreview()
    }
}

@Composable
private fun SearchBarPreview() {
    Column(modifier = Modifier.padding(8.dp)) {
        SearchBar("Material", onTextChanged = {}, modifier = Modifier.padding(bottom = 8.dp))
        SearchBar("", onTextChanged = {})
    }
}
