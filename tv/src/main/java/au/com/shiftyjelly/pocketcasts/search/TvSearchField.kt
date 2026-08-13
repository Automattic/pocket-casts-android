package au.com.shiftyjelly.pocketcasts.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvTile
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun TvSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var restoreRestFocus by remember { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val restFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { fieldFocusRequester.requestFocus() }
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            if (restoreRestFocus) {
                runCatching { restFocusRequester.requestFocus() }
                restoreRestFocus = false
            }
        }
    }

    if (editing) {
        var hasFocused by remember { mutableStateOf(false) }

        BackHandler(enabled = true) {
            editing = false
            restoreRestFocus = true
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.tvTypography.title3.copy(color = MaterialTheme.tvColors.textPrimary),
            cursorBrush = SolidColor(MaterialTheme.tvColors.textPrimary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    editing = false
                    restoreRestFocus = true
                },
            ),
            modifier = modifier
                .focusRequester(fieldFocusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        hasFocused = true
                    } else if (hasFocused) {
                        editing = false
                    }
                }
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        }

                        event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                            focusManager.moveFocus(FocusDirection.Up)
                            true
                        }

                        else -> false
                    }
                },
            decorationBox = { innerTextField ->
                TvSearchFieldContent(
                    query = query,
                    contentColor = MaterialTheme.tvColors.textPrimary,
                    innerTextField = innerTextField,
                )
            },
        )
    } else {
        TvTile(
            onClick = { editing = true },
            scale = CardDefaults.scale(focusedScale = 1f),
            shape = CardDefaults.shape(shape = RectangleShape),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.tvColors.textSecondary,
                focusedContainerColor = Color.Transparent,
                focusedContentColor = MaterialTheme.tvColors.textPrimary,
                pressedContainerColor = Color.Transparent,
            ),
            modifier = modifier.focusRequester(restFocusRequester),
        ) {
            TvSearchFieldContent(query = query, contentColor = LocalContentColor.current)
        }
    }
}

@Composable
private fun TvSearchFieldContent(
    query: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    innerTextField: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_search),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(LR.string.search),
                    style = MaterialTheme.tvTypography.title3,
                    color = MaterialTheme.tvColors.textSecondary,
                )
            }
            if (innerTextField != null) {
                innerTextField()
            } else if (query.isNotEmpty()) {
                Text(
                    text = query,
                    style = MaterialTheme.tvTypography.title3,
                    color = MaterialTheme.tvColors.textPrimary,
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchFieldPreview() {
    TvTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        ) {
            TvSearchField(query = "", onQueryChange = {})
        }
    }
}
