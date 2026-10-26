package au.com.shiftyjelly.pocketcasts.views.dialog

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun ConfirmationDialogContent(
    title: String?,
    summary: String?,
    icon: Drawable?,
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
        horizontalAlignment = FloorCenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom))
            .padding(start = 32.dp, top = 12.dp, end = 32.dp)
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 4.dp)
                .background(colors.primaryIcon02, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(16.dp))
        if (icon != null) {
            Spacer(Modifier.height(8.dp))
            FitCenterDrawable(icon, Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        if (title != null) {
            Text(
                text = title,
                color = colors.primaryText01,
                style = textViewStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.clipToBounds(),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (summary != null) {
            Text(
                text = summary,
                color = summaryColor,
                style = textViewStyle(
                    fontSize = summaryTextSize ?: 16.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.01).em,
                    roundFontSize = summaryTextSize == null,
                ),
                modifier = Modifier
                    .withoutLetterSpacingWidthPadding()
                    .clipToBounds(),
            )
        }
        Spacer(Modifier.height(24.dp))
        val confirmButton = @Composable {
            DialogButton(
                text = buttonType.text,
                textColor = colors.primaryInteractive02,
                backgroundColor = buttonType.color(),
                border = null,
                elevation = 2.dp,
                onClick = onConfirm,
            )
        }
        if (displayConfirmButtonFirst) {
            confirmButton()
        }
        if (secondaryType != null) {
            val secondaryColor = secondaryTextColor ?: secondaryType.color()
            DialogButton(
                text = secondaryType.text,
                textColor = secondaryColor,
                backgroundColor = colors.primaryUi01,
                border = if (removeSecondaryButtonBorder) null else BorderStroke(2.dp.toTruncatedPx(), secondaryColor),
                elevation = 0.dp,
                onClick = onSecondary,
            )
        }
        if (!displayConfirmButtonFirst) {
            confirmButton()
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DialogButton(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    border: BorderStroke?,
    elevation: Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp.toRoundedPx())
    val inset = 6.dp.toTruncatedPx()
    Box(
        contentAlignment = FloorCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = inset)
                .shadow(elevation, shape)
                .background(backgroundColor, shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .indication(interactionSource, ripple()),
        )
        Text(
            text = text,
            color = textColor,
            style = textViewStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = inset + 4.dp.toRoundedPx()),
        )
    }
}

@Composable
private fun FitCenterDrawable(drawable: Drawable, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = drawable.intrinsicWidth.toFloat()
        val height = drawable.intrinsicHeight.toFloat()
        val scale = min(size.width / width, size.height / height)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate((size.width - width * scale) / 2, (size.height - height * scale) / 2)
            canvas.scale(scale, scale)
            drawable.draw(canvas.nativeCanvas)
            canvas.restore()
        }
    }
}

@Composable
private fun ConfirmationDialog.ButtonType.color() = when (this) {
    is ConfirmationDialog.ButtonType.Danger -> MaterialTheme.theme.colors.support05
    is ConfirmationDialog.ButtonType.Normal -> MaterialTheme.theme.colors.support01
}

@Composable
private fun textViewStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
    roundFontSize: Boolean = true,
): TextStyle {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    return remember(fontSize, fontWeight, letterSpacing, roundFontSize, density) {
        val style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = fontWeight,
            fontSize = if (roundFontSize) with(density) { fontSize.roundToPx().toSp() } else fontSize,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            lineBreak = LineBreak.Paragraph,
            platformStyle = PlatformTextStyle(includeFontPadding = true),
        )
        val layout = textMeasurer.measure("A\nA\nA", style)
        val lineHeightPx = layout.getLineTop(2) - layout.getLineTop(1) + with(density) { 6.sp.roundToPx() }
        style.copy(
            lineHeight = with(density) { (lineHeightPx - 0.5f).toSp() },
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.Both),
        )
    }
}

@Composable
private fun Dp.toTruncatedPx(): Dp = with(LocalDensity.current) { toPx().toInt().toDp() }

@Composable
private fun Dp.toRoundedPx(): Dp = with(LocalDensity.current) { roundToPx().toDp() }

private fun Modifier.withoutLetterSpacingWidthPadding() = layout { measurable, constraints ->
    val desiredWidth = measurable.maxIntrinsicWidth(constraints.maxHeight) - 1
    val width = desiredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private val FloorCenterHorizontally = Alignment.Horizontal { size, space, _ -> (space - size) / 2 }

private val FloorCenter = Alignment { size, space, _ -> IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2) }

@Preview
@Composable
private fun ConfirmationDialogContentPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Box(Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
            ConfirmationDialogContent(
                title = "Apply to existing podcasts?",
                summary = "Would you like to change all your existing podcasts to show archived?",
                icon = AppCompatResources.getDrawable(LocalContext.current, IR.drawable.ic_archive),
                buttonType = ConfirmationDialog.ButtonType.Danger("Apply to existing"),
                secondaryType = ConfirmationDialog.ButtonType.Normal("No thanks"),
                onConfirm = {},
                onSecondary = {},
            )
        }
    }
}
