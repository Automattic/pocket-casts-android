package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.*
import okhttp3.internal.toImmutableList
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class DialogButtonState(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

enum class DialogButtonOrientation {
    Vertical,
    Horizontal
}

@Composable
fun DialogFrame(
    onDismissRequest: () -> Unit,
    title: String,
    buttons: List<DialogButtonState>,
    buttonOrientation: DialogButtonOrientation = DialogButtonOrientation.Horizontal,
    content: @Composable () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            backgroundColor = MaterialTheme.theme.colors.primaryUi01,
            elevation = 10.dp
        ) {
            Column {
                DialogTitle(title)
                Box(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    content()
                }
                DialogButtons(*buttons.toTypedArray(), orientation = buttonOrientation)
            }
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    TextH30(
        text = text,
        modifier = Modifier
            .padding(
                top = 24.dp,
                bottom = 12.dp
            )
            .padding(horizontal = 24.dp)
    )
}

@Composable
private fun DialogText(text: String) {
    TextP40(
        text = text,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .padding(horizontal = 24.dp)
    )
}

@Composable
private fun DialogButtons(
    vararg buttons: DialogButtonState,
    orientation: DialogButtonOrientation = DialogButtonOrientation.Horizontal,
) {
    if (buttons.isEmpty()) {
        return
    }

    val modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .selectableGroup()

    when (orientation) {
        DialogButtonOrientation.Vertical -> {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = modifier
            ) {
                buttons.forEach { DialogButton(it) }
            }
        }
        DialogButtonOrientation.Horizontal -> {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = modifier
            ) {
                buttons.forEach { DialogButton(it) }
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
    dismissDialog: () -> Unit,
) {
    var selected by remember { mutableStateOf(savedOption) }

    DialogFrame(
        title = title,
        buttons = listOf(
            DialogButtonState(
                text = stringResource(LR.string.cancel),
                onClick = dismissDialog
            ),
            DialogButtonState(
                text = stringResource(LR.string.ok),
                onClick = {
                    onSave(selected)
                    dismissDialog()
                }
            )
        ),
        onDismissRequest = dismissDialog,
    ) {
        Column {
            options.forEach { (item, itemLabel) ->
                DialogRadioButton(
                    text = itemLabel,
                    selected = selected == item,
                    onClick = { selected = item }
                )
            }
        }
    }
}

@Composable
private fun DialogRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
    ) {
        Spacer(Modifier.width(24.dp))
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(12.dp))
        TextP40(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun DialogButton(button: DialogButtonState) {
    TextButton(
        onClick = button.onClick,
        enabled = button.enabled
    ) {

        val buttonText =
            if (button.text == stringResource(LR.string.ok)) {
                button.text.uppercase(Locale.getDefault())
            } else {
                button.text
                    .lowercase(Locale.getDefault())
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }

        TextH40(
            text = buttonText,
            color = if (button.enabled) {
                MaterialTheme.theme.colors.primaryInteractive01
            } else {
                MaterialTheme.theme.colors.primaryInteractive01Disabled
            },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun ProgressDialog(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(
                    color = MaterialTheme.theme.colors.primaryInteractive02,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                TextP40(
                    text = text,
                    modifier = modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun <T> CheckboxDialog(
    title: String,
    options: List<Pair<T, String>>,
    savedOption: List<T>,
    maxOptions: Int,
    onSave: (List<T>) -> Unit,
    dismissDialog: () -> Unit,
) {
    var selected by remember { mutableStateOf(savedOption) }

    DialogFrame(
        title = title,
        buttons = listOf(
            DialogButtonState(
                text = stringResource(LR.string.cancel),
                onClick = dismissDialog
            ),
            DialogButtonState(
                text = stringResource(LR.string.ok),
                onClick = {
                    onSave(selected)
                    dismissDialog()
                }
            )
        ),
        onDismissRequest = dismissDialog,
    ) {
        Column {
            options.forEach { (item, itemLabel) ->
                DialogCheckBox(
                    text = itemLabel,
                    selected = selected.contains(item),
                    enabled = maxOptions > selected.size || selected.contains(item),
                    onClick = {
                        selected = if (selected.contains(item)) {
                            selected.toMutableList().apply {
                                remove(item)
                            }.toImmutableList()
                        } else {
                            selected.toMutableList().apply {
                                add(item)
                            }.toImmutableList()
                        }

                    }
                )
            }
        }
    }
}

@Composable
fun DialogCheckBox(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            )
    ) {
        Spacer(Modifier.width(24.dp))
        Checkbox(
            checked = selected,
            enabled = enabled,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                disabledColor = Color.Gray
            )
        )
        Spacer(Modifier.width(12.dp))
        TextP40(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun DialogFramePreview(
    theme: Theme.ThemeType = Theme.ThemeType.LIGHT,
    orientation: DialogButtonOrientation,
) {
    AppTheme(theme) {
        DialogFrame(
            title = "Title",
            buttons = listOf(
                DialogButtonState(text = "no", onClick = {}),
                DialogButtonState(text = "disabled", enabled = false, onClick = {}),
                DialogButtonState(text = "yEs", onClick = {}),
            ),
            buttonOrientation = orientation,
            onDismissRequest = {}
        ) {
            DialogText("All the information you wanted to know about everything.")
        }
    }
}

@Preview
@Composable
private fun DialogFramePreview_horizontal() =
    DialogFramePreview(orientation = DialogButtonOrientation.Horizontal, theme = Theme.ThemeType.LIGHT)

@Preview
@Composable
private fun DialogFramePreview_vertical() =
    DialogFramePreview(orientation = DialogButtonOrientation.Vertical, theme = Theme.ThemeType.DARK)

@Composable
private fun RadioDialogPreview(theme: Theme.ThemeType) {
    AppTheme(theme) {
        RadioDialog(
            title = "Title",
            options = listOf(
                Pair(true, stringResource(LR.string.player_up_next_empty_desc)),
                Pair(false, stringResource(LR.string.player_up_next_clear)),
            ),
            savedOption = true,
            onSave = {},
            dismissDialog = {}
        )
    }
}

@Preview
@Composable
private fun RadioDialogPreview_light() = RadioDialogPreview(Theme.ThemeType.LIGHT)

@Preview
@Composable
private fun RadioDialogPreview_dark() = RadioDialogPreview(Theme.ThemeType.DARK)

@Composable
private fun CheckboxDialogPreview(theme: Theme.ThemeType) {
    AppTheme(theme) {
        CheckboxDialog(
            title = "Title", 
            options = listOf(
                Pair("Star", stringResource(id = LR.string.settings_media_notification_controls_title_star)),
                Pair("Archive", stringResource(id = LR.string.settings_media_notification_controls_title_archive)),
                Pair("PlayNext", stringResource(id = LR.string.settings_media_notification_controls_title_play_next))
            ),
            savedOption = listOf("Archive", "PlayNext"),
            maxOptions = 2,
            onSave = {},
            dismissDialog = {}
        )
    }
}

@Preview
@Composable
private fun CheckboxDialogPreview_light() = CheckboxDialogPreview(Theme.ThemeType.LIGHT)

@Preview
@Composable
private fun CheckboxDialogPreview_dark() = CheckboxDialogPreview(Theme.ThemeType.DARK)

@Preview(showBackground = true)
@Composable
private fun ProgressDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType
) {
    AppTheme(themeType) {
        ProgressDialog(
            text = "In Progress",
            onDismiss = {}
        )
    }
}
