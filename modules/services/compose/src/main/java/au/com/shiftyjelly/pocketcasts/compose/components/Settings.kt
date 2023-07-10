package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.horizontalPadding
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.indentedStartPadding
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.verticalPadding
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR

object SettingsSection {
    val horizontalPadding = 24.dp
    val indentedStartPadding = horizontalPadding + 48.dp
    val verticalPadding = 12.dp
}

sealed class SettingRowToggle {
    data class Checkbox(val checked: Boolean, val enabled: Boolean = true) : SettingRowToggle()
    data class Switch(val checked: Boolean, val enabled: Boolean = true) : SettingRowToggle()
    object None : SettingRowToggle()
}

@Composable
fun SettingSection(
    modifier: Modifier = Modifier,
    heading: String? = null,
    indent: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = verticalPadding)
        ) {
            if (heading != null) {
                TextH40(
                    text = heading,
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                    modifier = Modifier.padding(
                        start = if (indent) indentedStartPadding else horizontalPadding,
                        end = horizontalPadding,
                        top = verticalPadding,
                        bottom = verticalPadding
                    )
                )
            }
            content()
        }
        HorizontalDivider()
    }
}

@Composable
fun <T> SettingRadioDialogRow(
    primaryText: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    options: List<T>,
    savedOption: T,
    optionToLocalisedString: (T) -> String,
    onSave: (T) -> Unit,
) {

    var showDialog by remember { mutableStateOf(false) }
    SettingRow(
        primaryText = primaryText,
        secondaryText = secondaryText,
        modifier = modifier.clickable { showDialog = true }
    ) {
        if (showDialog) {
            RadioDialog(
                title = primaryText,
                options = options.map { Pair(it, optionToLocalisedString(it)) },
                savedOption = savedOption,
                onSave = onSave,
                dismissDialog = { showDialog = false }
            )
        }
    }
}

/*
 * Click handling should be done in the modifier passed to this composable to ensure the
 * entire row is clickable.
 */
@Composable
fun SettingRow(
    primaryText: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    icon: GradientIconData? = null,
    @DrawableRes primaryTextEndDrawable: Int? = null,
    toggle: SettingRowToggle = SettingRowToggle.None,
    indent: Boolean = true,
    additionalContent: @Composable () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                end = horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(if (indent || icon != null) indentedStartPadding else horizontalPadding)
        ) {
            GradientIcon(icon)
        }
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextP40(
                    text = primaryText,
                    color = MaterialTheme.theme.colors.primaryText01,
                )

                if (primaryTextEndDrawable != null) {
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(primaryTextEndDrawable),
                        contentDescription = null
                    )
                }
            }

            if (secondaryText != null) {
                Spacer(Modifier.height(4.dp))
                Crossfade(targetState = secondaryText) { text ->
                    TextP50(
                        text = text,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.theme.colors.primaryText02,
                    )
                }
            }
            additionalContent()
        }

        when (toggle) {
            is SettingRowToggle.Checkbox -> {
                Spacer(Modifier.width(12.dp))
                Checkbox(
                    checked = toggle.checked,
                    enabled = toggle.enabled,
                    onCheckedChange = null,
                )
            }
            is SettingRowToggle.Switch -> {
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = toggle.checked,
                    enabled = toggle.enabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray,
                    )
                )
            }
            SettingRowToggle.None -> { /* Nothing */
            }
        }
    }
}

@Preview
@Composable
private fun SettingSectionPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType
) {
    AppTheme(theme) {
        SettingSection(heading = "Section Heading") {
            SettingRow(primaryText = "Row with just primary text")
            SettingRow(
                primaryText = "Row with primary text and secondary text",
                secondaryText = "Because secondary text is great and why wouldn't you want it?!?!",
            )
            SettingRow(
                primaryText = "Row with switch",
                toggle = SettingRowToggle.Switch(checked = true)
            )
            SettingRow(
                primaryText = "Row with checkbox",
                toggle = SettingRowToggle.Checkbox(checked = true)
            )
            SettingRow(
                primaryText = "Such very very very very very very very long text",
                secondaryText = "I know you want to flip this toggle, so just do it. DO IT!",
                toggle = SettingRowToggle.Switch(checked = false),
                icon = GradientIconData(
                    res = IR.drawable.ic_podcasts,
                    colors = listOf(Color.Red, Color.Yellow)
                ),
                primaryTextEndDrawable = IR.drawable.ic_effects_plus,
            )
        }
    }
}

@ShowkaseComposable(name = "SettingRow", group = "Setting", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun SettingRowLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Primary text",
            secondaryText = "Secondary text",
        )
    }
}

@ShowkaseComposable(name = "SettingRow", group = "Setting", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun SettingRowDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SettingRow(
            primaryText = "Primary text",
            secondaryText = "Secondary text",
        )
    }
}

@ShowkaseComposable(name = "SettingRow", group = "Setting", styleName = "Icon")
@Preview(name = "Icon")
@Composable
fun SettingRowIconPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with icon",
            icon = GradientIconData(IR.drawable.ic_profile_settings)
        )
    }
}

@ShowkaseComposable(name = "SettingRow", group = "Setting", styleName = "Switch")
@Preview(name = "Switch")
@Composable
fun SettingRowSwitchPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with switch",
            toggle = SettingRowToggle.Switch(checked = true)
        )
    }
}

@ShowkaseComposable(name = "SettingRow", group = "Setting", styleName = "Checkbox")
@Preview(name = "Checkbox")
@Composable
fun SettingRowCheckboxPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with checkbox",
            toggle = SettingRowToggle.Checkbox(checked = true)
        )
    }
}

@ShowkaseComposable(name = "SettingSection", group = "Setting", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun SettingSectionLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingSection(heading = "Section heading") {
            SettingRow(primaryText = "Setting row")
        }
    }
}

@ShowkaseComposable(name = "SettingSection", group = "Setting", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun SettingSectionDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SettingSection(heading = "Section heading") {
            SettingRow(primaryText = "Setting row")
        }
    }
}
