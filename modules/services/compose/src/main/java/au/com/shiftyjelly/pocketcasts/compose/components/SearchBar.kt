package au.com.shiftyjelly.pocketcasts.compose.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.removeNewLines
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object SearchBarDefaults {
    @Composable
    fun colors(
        cursorColor: Color = MaterialTheme.theme.colors.primaryText02,
        textColor: Color = MaterialTheme.theme.colors.primaryText01,
        disabledTextColor: Color = MaterialTheme.theme.colors.primaryText01,
        placeholderColor: Color = MaterialTheme.theme.colors.primaryText02,
        disabledPlaceholderColor: Color = MaterialTheme.theme.colors.primaryText02,
        leadingIconColor: Color = MaterialTheme.theme.colors.primaryIcon02,
        disabledLeadingIconColor: Color = MaterialTheme.theme.colors.primaryIcon02,
        trailingIconColor: Color = MaterialTheme.theme.colors.primaryIcon02,
        disabledTrailingIconColor: Color = MaterialTheme.theme.colors.primaryIcon02,
        backgroundColor: Color = MaterialTheme.theme.colors.primaryField01,
        focusedBorderColor: Color = Color.Transparent,
        unfocusedBorderColor: Color = Color.Transparent,
        disabledBorderColorColor: Color = Color.Transparent,
    ) = TextFieldDefaults.outlinedTextFieldColors(
        cursorColor = cursorColor,
        textColor = textColor,
        disabledTextColor = disabledTextColor,
        placeholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        leadingIconColor = leadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        trailingIconColor = trailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        backgroundColor = backgroundColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColorColor,
    )
}

enum class SearchBarStyle {
    Small,
    Regular,
    ;

    internal val backgroundShape
        get() = when (this) {
            Small -> RoundedCornerShape(8.dp)
            Regular -> RoundedCornerShape(10.dp)
        }

    @get:Composable
    internal val textStyle
        get() = when (this) {
            Small -> LocalTextStyle.current.copy(
                fontSize = 14.sp,
            )

            Regular -> LocalTextStyle.current
        }

    private val iconBoxSize
        get() = when (this) {
            Small -> 32.dp
            Regular -> 42.dp
        }

    private val iconSize
        get() = when (this) {
            Small -> 16.dp
            Regular -> 24.dp
        }

    @Composable
    internal fun LeadingIcon() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconBoxSize),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }

    @Composable
    internal fun TrailingIcon(onClick: () -> Unit) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconBoxSize)
                .clickable(onClick = onClick),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_cancel),
                contentDescription = stringResource(LR.string.clear),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = stringResource(LR.string.search_podcasts_or_add_url),
    textStyle: TextStyle = LocalTextStyle.current,
    cornerRadius: Dp = 10.dp,
    colors: TextFieldColors = SearchBarDefaults.colors(),
    contentPadding: PaddingValues = TextFieldDefaults.textFieldWithoutLabelPadding(
        top = 0.dp,
        bottom = 0.dp,
    ),
    onClickClear: () -> Unit = {},
    onSearch: () -> Unit = {},
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val shape = RoundedCornerShape(cornerRadius)
    BasicTextField(
        value = text,
        onValueChange = {
            onTextChange(it.removeNewLines())
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
                leadingIcon = leadingContent ?: {
                    Icon(
                        painter = painterResource(IR.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = trailingContent ?: {
                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onClickClear()
                                onTextChange("")
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
                contentPadding = contentPadding,
                shape = shape,
            )
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchBar(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    style: SearchBarStyle = SearchBarStyle.Regular,
    colors: TextFieldColors = SearchBarDefaults.colors(),
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        imeAction = ImeAction.Search,
    ),
    contentPadding: PaddingValues = TextFieldDefaults.textFieldWithoutLabelPadding(
        top = 0.dp,
        bottom = 0.dp,
    ),
    onClickClear: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val textStyle = style.textStyle.merge(
        color = colors.textColor(enabled).value,
    )

    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        BasicTextField(
            state = state,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = {
                onSearch()
                focusManager.clearFocus()
            },
            lineLimits = lineLimits,
            enabled = enabled,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.cursorColor(false).value),
            modifier = modifier.background(colors.backgroundColor(enabled).value, style.backgroundShape),
            decorator = object : TextFieldDecorator {
                @Composable
                override fun Decoration(innerTextField: @Composable (() -> Unit)) {
                    TextFieldDefaults.OutlinedTextFieldDecorationBox(
                        value = state.text.toString(),
                        innerTextField = innerTextField,
                        placeholder = {
                            Text(
                                text = placeholder,
                                fontSize = textStyle.fontSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            style.LeadingIcon()
                        },
                        trailingIcon = {
                            if (state.text.isNotEmpty()) {
                                style.TrailingIcon(
                                    onClick = {
                                        onClickClear()
                                        state.clearText()
                                        focusManager.clearFocus()
                                    },
                                )
                            }
                        },
                        enabled = enabled,
                        colors = colors,
                        singleLine = true,
                        interactionSource = remember { MutableInteractionSource() },
                        visualTransformation = VisualTransformation.None,
                        contentPadding = contentPadding,
                        shape = style.backgroundShape,
                    )
                }
            },
        )
    }
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
            onTextChange = {},
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            Modifier
                .matchParentSize() // cover SearchBar
                .clickable { onClick() }, // handle click events
        )
    }
}

@Preview
@Composable
private fun SearchBarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            SearchBar(
                state = rememberTextFieldState(initialText = "Search term"),
                modifier = Modifier.fillMaxWidth(),
            )
            SearchBar(
                state = rememberTextFieldState(),
                placeholder = "Search…",
                modifier = Modifier.fillMaxWidth(),
            )
            SearchBar(
                state = rememberTextFieldState(initialText = "Small search term"),
                style = SearchBarStyle.Small,
                modifier = Modifier.fillMaxWidth(),
            )
            SearchBar(
                state = rememberTextFieldState(),
                placeholder = "Small search…",
                style = SearchBarStyle.Small,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
