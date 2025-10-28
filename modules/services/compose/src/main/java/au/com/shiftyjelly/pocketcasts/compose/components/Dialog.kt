@file:Suppress("ktlint:compose:modifier-missing-check")

package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class DialogComponents internal constructor() {
    @Composable
    fun DialogTitle(
        text: String,
        modifier: Modifier = Modifier,
    ) {
        TextH30(
            text = text,
            modifier = modifier,
        )
    }

    @Composable
    fun DialogText(
        text: String,
        modifier: Modifier = Modifier,
    ) {
        TextP40(
            text = text,
            modifier = modifier,
        )
    }

    @Composable
    fun DialogButton(
        properties: DialogButtonProperties,
        modifier: Modifier = Modifier,
    ) {
        TextButton(
            enabled = properties.enabled,
            onClick = properties.onClick,
            modifier = modifier,
        ) {
            TextH40(
                text = remember(properties.text) {
                    val locale = Locale.getDefault()
                    val lowerCaseText = properties.text.lowercase(locale)
                    if (lowerCaseText == "ok") {
                        properties.text.uppercase(Locale.getDefault())
                    } else {
                        lowerCaseText.replaceFirstChar {
                            it.titlecase(Locale.getDefault())
                        }
                    }
                },
                color = if (properties.enabled) {
                    MaterialTheme.theme.colors.primaryInteractive01
                } else {
                    MaterialTheme.theme.colors.primaryInteractive01Disabled
                },
            )
        }
    }
}

data class DialogButtonProperties(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun BaseDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable DialogComponents.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Card {
            val components = remember { DialogComponents() }
            content(components)
        }
    }
}

@Composable
fun ProgressDialog(
    text: String,
    onDismissRequest: () -> Unit = {},
) {
    BaseDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(24.dp),
        ) {
            CircularProgressIndicator()

            DialogText(
                text = text,
            )
        }
    }
}

@Composable
fun SimpleDialog(
    title: String,
    body: String,
    buttonProperties: List<DialogButtonProperties>,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
) {
    BaseDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(
                top = 24.dp,
                bottom = if (buttonProperties.isEmpty()) 24.dp else 8.dp,
            ),
        ) {
            DialogTitle(
                text = title,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            DialogText(
                text = body,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            if (buttonProperties.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 4.dp),
                ) {
                    buttonProperties.forEach { properties ->
                        DialogButton(
                            properties = properties,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormFieldDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    keyboardType: KeyboardType,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    isSaveEnabled: (String) -> Boolean = { true },
) {
    BaseDialog(
        onDismissRequest = onDismissRequest,
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        val textFieldState = rememberTextFieldState(initialValue)

        val onConfirm = {
            val value = textFieldState.text.toString()
            if (isSaveEnabled(value)) {
                onConfirm(value)
                onDismissRequest()
            }
        }

        val buttons = listOf(
            DialogButtonProperties(
                text = stringResource(LR.string.cancel),
                onClick = onDismissRequest,
            ),
            DialogButtonProperties(
                text = stringResource(LR.string.ok),
                enabled = isSaveEnabled(textFieldState.text.toString()),
                onClick = onConfirm,
            ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(
                top = 24.dp,
                bottom = 8.dp,
            ),
        ) {
            DialogTitle(
                text = title,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            FormField(
                state = textFieldState,
                placeholder = placeholder,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                onImeAction = onConfirm,
                label = { Text(placeholder) },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 24.dp),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 4.dp),
            ) {
                buttons.forEach { properties ->
                    DialogButton(
                        properties = properties,
                    )
                }
            }
        }
    }
}

@Composable
fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    savedOption: T,
    onSave: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseDialog(
        onDismissRequest = onDismissRequest,
    ) {
        var selected by remember { mutableStateOf(savedOption) }
        val buttons = listOf(
            DialogButtonProperties(
                text = stringResource(LR.string.cancel),
                onClick = onDismissRequest,
            ),
            DialogButtonProperties(
                text = stringResource(LR.string.ok),
                onClick = {
                    onSave(selected)
                    onDismissRequest()
                },
            ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(
                top = 24.dp,
                bottom = 8.dp,
            ),
        ) {
            DialogTitle(
                text = title,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (item, itemLabel) ->
                    DialogRadioButton(
                        text = itemLabel,
                        selected = selected == item,
                        onClick = { selected = item },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 4.dp),
            ) {
                buttons.forEach { properties ->
                    DialogButton(
                        properties = properties,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 24.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        TextP40(
            text = text,
        )
    }
}

@Preview
@Composable
private fun SimpleDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        SimpleDialog(
            title = "Title",
            body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            buttonProperties = listOf(
                DialogButtonProperties(
                    text = "cancel",
                    onClick = {},
                ),
                DialogButtonProperties(
                    text = "ok",
                    onClick = {},
                ),
            ),
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun FormFieldDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        FormFieldDialog(
            title = "Title",
            placeholder = "Placeholder",
            initialValue = "Value",
            keyboardType = KeyboardType.Number,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun RadioDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        RadioDialog(
            title = "Title",
            options = listOf(
                Pair(true, stringResource(LR.string.player_up_next_empty_desc)),
                Pair(false, stringResource(LR.string.player_up_next_clear)),
            ),
            savedOption = true,
            onSave = {},
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun ProgressDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ProgressDialog(
            text = "In Progress",
        )
    }
}
