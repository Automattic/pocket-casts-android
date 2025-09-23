package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.horizontalPadding
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.indentedStartPadding
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection.verticalPadding
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlinx.coroutines.delay
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
    subHeading: String? = null,
    indent: Boolean = true,
    showDivider: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = verticalPadding),
        ) {
            if (heading != null) {
                SettingSectionHeader(
                    text = heading,
                    indent = indent,
                )
            }
            if (subHeading != null) {
                TextP50(
                    text = subHeading,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(
                        start = if (indent) indentedStartPadding else horizontalPadding,
                        end = horizontalPadding,
                        top = if (heading == null) verticalPadding else 0.dp,
                        bottom = verticalPadding,
                    ),
                )
            }
            content()
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Composable
fun SettingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    indent: Boolean = true,
    paddingValues: PaddingValues = PaddingValues(
        start = if (indent) indentedStartPadding else horizontalPadding,
        end = horizontalPadding,
        top = verticalPadding,
        bottom = verticalPadding,
    ),
) {
    TextH40(
        text = text,
        color = MaterialTheme.theme.colors.primaryInteractive01,
        modifier = modifier.padding(paddingValues),
    )
}

@Composable
fun <T> SettingRadioDialogRow(
    primaryText: String,
    options: List<T>,
    savedOption: T,
    optionToLocalisedString: (T) -> String,
    onSave: (T) -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    icon: Painter? = null,
    iconGradientColors: List<Color>? = null,
    indent: Boolean = true,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingRow(
        primaryText = primaryText,
        secondaryText = secondaryText,
        icon = icon,
        iconGradientColors = iconGradientColors,
        modifier = modifier.clickable(enabled = enabled) { showDialog = true }.then(
            if (!enabled) {
                Modifier.graphicsLayer(alpha = .38f)
            } else {
                Modifier
            },
        ),
        indent = indent,
    ) {
        if (showDialog) {
            RadioDialog(
                title = primaryText,
                options = options.map { Pair(it, optionToLocalisedString(it)) },
                savedOption = savedOption,
                onSave = onSave,
                dismissDialog = { showDialog = false },
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
    icon: Painter? = null,
    iconTint: Color? = null,
    iconGradientColors: List<Color>? = null,
    @DrawableRes primaryTextEndDrawable: Int? = null,
    toggle: SettingRowToggle = SettingRowToggle.None,
    indent: Boolean = true,
    enabled: Boolean = true,
    showFlashWithDelay: Duration? = null, // if null, no flash is shown
    horizontalPadding: Dp = SettingsSection.horizontalPadding,
    verticalPadding: Dp = SettingsSection.verticalPadding,
    additionalContent: @Composable () -> Unit = {},
) {
    var flashAlphaTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(showFlashWithDelay) {
        if (showFlashWithDelay != null) {
            delay(showFlashWithDelay)
            flashAlphaTarget = 1.0f
        }
    }
    val flashAlpha by animateFloatAsState(
        targetValue = flashAlphaTarget,
        finishedListener = {
            flashAlphaTarget = 0f
        },
    )

    val backgroundColor = MaterialTheme.theme.colors.primaryInteractive01Active.copy(
        alpha = flashAlpha,
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(backgroundColor)
            .padding(
                end = horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding,
            ).then(
                if (!enabled) {
                    Modifier.graphicsLayer(alpha = .38f)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(if (indent || icon != null) indentedStartPadding else horizontalPadding),
        ) {
            if (icon != null) {
                if (iconGradientColors != null) {
                    GradientIcon(
                        painter = icon,
                        colors = iconGradientColors,
                    )
                } else {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = iconTint ?: MaterialTheme.theme.colors.primaryInteractive01,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextP40(
                    text = primaryText,
                    color = MaterialTheme.theme.colors.primaryText01,
                )

                if (primaryTextEndDrawable != null) {
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(primaryTextEndDrawable),
                        contentDescription = null,
                    )
                }
            }

            if (secondaryText != null) {
                Spacer(Modifier.height(4.dp))
                TextP50(
                    text = secondaryText,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
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
                    ),
                )
            }
            SettingRowToggle.None -> {
                /* Nothing */
            }
        }
    }
}

@Composable
fun SettingInfoRow(
    text: String,
    modifier: Modifier = Modifier,
    indent: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .padding(
                start = if (indent) indentedStartPadding else horizontalPadding,
                end = horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
    ) {
        TextP50(
            text = text,
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Preview
@Composable
private fun SettingSectionPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
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
                toggle = SettingRowToggle.Switch(checked = true),
            )
            SettingRow(
                primaryText = "Row with checkbox",
                toggle = SettingRowToggle.Checkbox(checked = true),
            )
            SettingRow(
                primaryText = "Such very very very very very very very long text",
                secondaryText = "I know you want to flip this toggle, so just do it. DO IT!",
                toggle = SettingRowToggle.Switch(checked = false),
                icon = painterResource(IR.drawable.ic_podcasts),
                iconGradientColors = listOf(Color.Red, Color.Yellow),
                primaryTextEndDrawable = IR.drawable.ic_effects_plus,
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun SettingRowLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Primary text",
            secondaryText = "Secondary text",
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun SettingRowDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SettingRow(
            primaryText = "Primary text",
            secondaryText = "Secondary text",
        )
    }
}

@Preview(name = "Icon (Project)")
@Composable
private fun SettingRowIconProjectPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with icon",
            icon = painterResource(IR.drawable.ic_profile_settings),
        )
    }
}

@Preview(name = "Icon (Material)")
@Composable
private fun SettingRowIconMaterialPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with icon",
            icon = rememberVectorPainter(Icons.Default.Share),
        )
    }
}

@Preview(name = "Icon (Gradient)")
@Composable
private fun SettingRowIconGradientPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with icon",
            icon = painterResource(IR.drawable.ic_podcasts),
            iconGradientColors = listOf(Color.Red, Color.Yellow),
        )
    }
}

@Preview(name = "Switch")
@Composable
private fun SettingRowSwitchPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with switch",
            toggle = SettingRowToggle.Switch(checked = true),
        )
    }
}

@Preview(name = "Checkbox")
@Composable
private fun SettingRowCheckboxPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingRow(
            primaryText = "Row with checkbox",
            toggle = SettingRowToggle.Checkbox(checked = true),
        )
    }
}

@Preview(name = "Light")
@Composable
private fun SettingSectionLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingSection(heading = "Section heading") {
            SettingRow(primaryText = "Setting row")
        }
    }
}

@Preview(name = "Dark")
@Composable
private fun SettingSectionDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SettingSection(heading = "Section heading") {
            SettingRow(primaryText = "Setting row")
        }
    }
}

@Preview(name = "Info")
@Composable
private fun SettingInfoRowPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SettingInfoRow(
            text = "The quick brown fox jumps over the lazy dog",
        )
    }
}
