package au.com.shiftyjelly.pocketcasts.views.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun ConfirmationDialogContent(
    title: String?,
    summary: String?,
    @DrawableRes iconRes: Int?,
    iconTint: Color?,
    buttonType: ConfirmationDialog.ButtonType,
    secondaryType: ConfirmationDialog.ButtonType?,
    onConfirm: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    summaryColor: Color = MaterialTheme.theme.colors.primaryText02,
    summaryTextSize: TextUnit? = null,
    secondaryTextColor: Color? = null,
    displayConfirmButtonFirst: Boolean = false,
    removeSecondaryButtonBorder: Boolean = false,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom))
            .padding(start = 32.dp, top = 12.dp, end = 32.dp),
    ) {
        Pill()
        Spacer(Modifier.height(16.dp))
        if (iconRes != null) {
            Spacer(Modifier.height(8.dp))
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint ?: Color.Unspecified,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (title != null) {
            TextH20(
                text = title,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (summary != null) {
            TextP40(
                text = summary,
                color = summaryColor,
                fontSize = summaryTextSize ?: 16.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        val confirmButton = @Composable {
            RowButton(
                text = buttonType.text,
                onClick = onConfirm,
                includePadding = false,
                colors = ButtonDefaults.buttonColors(backgroundColor = buttonType.color()),
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        if (displayConfirmButtonFirst) {
            confirmButton()
        }
        if (secondaryType != null) {
            val secondaryColor = secondaryTextColor ?: secondaryType.color()
            val secondaryColors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.primaryUi01, contentColor = secondaryColor)
            if (removeSecondaryButtonBorder) {
                RowTextButton(
                    text = secondaryType.text,
                    onClick = onSecondary,
                    includePadding = false,
                    colors = secondaryColors,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            } else {
                RowOutlinedButton(
                    text = secondaryType.text,
                    onClick = onSecondary,
                    includePadding = false,
                    border = BorderStroke(2.dp, secondaryColor),
                    colors = secondaryColors,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
        if (!displayConfirmButtonFirst) {
            confirmButton()
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConfirmationDialog.ButtonType.color() = when (this) {
    is ConfirmationDialog.ButtonType.Danger -> MaterialTheme.theme.colors.support05
    is ConfirmationDialog.ButtonType.Normal -> MaterialTheme.theme.colors.support01
}

@Preview
@Composable
private fun ConfirmationDialogContentPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Box(Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
            ConfirmationDialogContent(
                title = "Apply to existing podcasts?",
                summary = "Would you like to change all your existing podcasts to show archived?",
                iconRes = IR.drawable.ic_archive,
                iconTint = MaterialTheme.theme.colors.primaryIcon01,
                buttonType = ConfirmationDialog.ButtonType.Danger("Apply to existing"),
                secondaryType = ConfirmationDialog.ButtonType.Normal("No thanks"),
                onConfirm = {},
                onSecondary = {},
            )
        }
    }
}
