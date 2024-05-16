package au.com.shiftyjelly.pocketcasts.compose.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchBar(
    text: String,
    onTextChanged: (String) -> Unit,
    placeholder: String = stringResource(LR.string.search_podcasts_or_add_url),
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val focusManager = LocalFocusManager.current
    val colors = TextFieldDefaults.outlinedTextFieldColors(
        cursorColor = MaterialTheme.theme.colors.primaryText02,
        textColor = MaterialTheme.theme.colors.primaryText01,
        disabledTextColor = MaterialTheme.theme.colors.primaryText01,
        placeholderColor = MaterialTheme.theme.colors.primaryText02,
        disabledPlaceholderColor = MaterialTheme.theme.colors.primaryText02,
        leadingIconColor = MaterialTheme.theme.colors.primaryIcon02,
        disabledLeadingIconColor = MaterialTheme.theme.colors.primaryIcon02,
        trailingIconColor = MaterialTheme.theme.colors.primaryIcon02,
        disabledTrailingIconColor = MaterialTheme.theme.colors.primaryIcon02,
        backgroundColor = MaterialTheme.theme.colors.primaryField01,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
    )
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = text,
        onValueChange = {
            onTextChanged(it.removeNewLines())
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch()
                focusManager.clearFocus()
            },
        ),
        maxLines = 1,
        enabled = enabled,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(false).value),
        modifier = modifier
            .onKeyEvent {
                // close the keyboard on enter
                if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    focusManager.clearFocus()
                    true
                } else {
                    false
                }
            }
            .background(colors.backgroundColor(enabled).value, shape)
            .defaultMinSize(
                minWidth = TextFieldDefaults.MinWidth,
                minHeight = 42.dp,
            ),
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = text,
                innerTextField = innerTextField,
                placeholder = {
                    Text(
                        placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(IR.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onTextChanged("")
                                focusManager.clearFocus()
                            },
                        ) {
                            Icon(
                                painter = painterResource(IR.drawable.ic_cancel),
                                contentDescription = stringResource(LR.string.cancel),
                            )
                        }
                    }
                },
                enabled = enabled,
                colors = colors,
                singleLine = true,
                interactionSource = remember { MutableInteractionSource() },
                visualTransformation = VisualTransformation.None,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
            )
        },
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
            },
    ) {
        SearchBar(
            text = "",
            placeholder = text,
            onTextChanged = {},
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            Modifier
                .matchParentSize() // cover SearchBar
                .clickable { onClick() }, // handle click events
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
