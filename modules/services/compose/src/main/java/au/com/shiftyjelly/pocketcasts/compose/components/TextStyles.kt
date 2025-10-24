package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Locale

@Composable
fun TextH10(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    disableAutoScale: Boolean = false,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = 31.sp,
    fontWeight: FontWeight = FontWeight.W700,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = 37.sp,
    textAlign: TextAlign? = null,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
fun TextH20(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    disableAutoScale: Boolean = false,
    fontSize: TextUnit = 22.sp,
    lineHeight: TextUnit = 30.sp,
    letterSpacing: TextUnit = 0.sp,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        letterSpacing = letterSpacing.scaled(disableAutoScale, fontScale),
        fontWeight = FontWeight.W700,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
fun TextH20(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    disableAutoScale: Boolean = false,
    fontSize: TextUnit = 22.sp,
    lineHeight: TextUnit = 30.sp,
    fontScale: Float = 1f,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        fontWeight = FontWeight.W700,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        inlineContent = inlineContent,
        modifier = modifier,
    )
}

@Composable
fun TextH30(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    fontSize: TextUnit? = null,
    lineHeight: TextUnit = 21.sp,
    letterSpacing: TextUnit = 0.sp,
    style: TextStyle = TextStyle(),
    fontScale: Float = 1f,
) {
    val fontSizeUpdated = fontSize ?: 18.sp
    Text(
        text = text,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSizeUpdated.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        letterSpacing = letterSpacing.scaled(disableAutoScale, fontScale),
        textAlign = textAlign,
        fontWeight = fontWeight ?: FontWeight.W600,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
        modifier = modifier,
    )
}

@Composable
fun TextP30(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = FontWeight.W500,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = 18.sp.scaled(disableAutoScale, fontScale),
        lineHeight = 24.sp.scaled(disableAutoScale, fontScale),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

@Composable
fun TextH40(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight = FontWeight.W500,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    lineHeight: TextUnit = 21.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = 15.sp.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextH40(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight = FontWeight.W500,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    lineHeight: TextUnit = 21.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = 15.sp.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextP40(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 22.sp,
    fontScale: Float = 1f,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        modifier = modifier,
    )
}

@Composable
fun TextP40(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 22.sp,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

@Composable
fun TextH50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.W500,
    disableAutoScale: Boolean = false,
    lineHeight: TextUnit = 20.sp,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextP50(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    maxLines: Int? = null,
    style: TextStyle? = null,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit? = null,
    letterSpacing: TextUnit? = null,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
) {
    TextP50(
        text = AnnotatedString(text),
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        style = style,
        textAlign = textAlign,
        fontWeight = fontWeight,
        disableAutoScale = disableAutoScale,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontScale = fontScale,
    )
}

@Composable
fun TextP50(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color? = null,
    maxLines: Int? = null,
    style: TextStyle? = null,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit? = null,
    letterSpacing: TextUnit? = null,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
) {
    val fontSize = 14.sp
    val lineHeightUpdated = lineHeight ?: 20.sp
    val letterSpacingUpdated = letterSpacing ?: 0.sp

    Text(
        text = text,
        color = color ?: MaterialTheme.theme.colors.primaryText01,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeightUpdated.scaled(disableAutoScale, fontScale),
        letterSpacing = letterSpacingUpdated.scaled(disableAutoScale, fontScale),
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        style = style ?: LocalTextStyle.current,
        textAlign = textAlign,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

val textH60FontSize = 12.sp

@Composable
fun TextH60(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontSize: TextUnit = textH60FontSize,
    fontWeight: FontWeight = FontWeight.W600,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = 14.sp.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        textAlign = textAlign,
    )
}

@Composable
fun TextP60(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null,
    style: TextStyle = TextStyle(),
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    letterSpacing: TextUnit = 0.sp,
    lineHeight: TextUnit = 15.sp,
) {
    val fontSizeUpdated = fontSize ?: 13.sp
    Text(
        text = text,
        color = color,
        fontSize = fontSizeUpdated.scaled(disableAutoScale, fontScale),
        lineHeight = lineHeight.scaled(disableAutoScale, fontScale),
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = fontWeight,
        style = style,
        modifier = modifier,
    )
}

@Composable
fun TextP60(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null,
    style: TextStyle = TextStyle(),
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    letterSpacing: TextUnit = 0.sp,
) {
    val fontSizeUpdated = fontSize ?: 13.sp
    Text(
        text = text,
        color = color,
        fontSize = fontSizeUpdated.scaled(disableAutoScale, fontScale),
        lineHeight = 15.sp.scaled(disableAutoScale, fontScale),
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = fontWeight,
        style = style,
        modifier = modifier,
    )
}

@Composable
fun TextH70(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.W500,
    maxLines: Int = Int.MAX_VALUE,
    fontSize: TextUnit? = null,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
) {
    val fontSizeUpdated = fontSize ?: 12.sp
    Text(
        text = text,
        color = color,
        fontSize = fontSizeUpdated.scaled(disableAutoScale, fontScale),
        lineHeight = 14.sp.scaled(disableAutoScale, fontScale),
        letterSpacing = 0.25.sp.scaled(disableAutoScale, fontScale),
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        textAlign = textAlign,
    )
}

@Composable
fun TextC50(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText02,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    fontWeight: FontWeight = FontWeight.W700,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        color = color,
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp.scaled(disableAutoScale, fontScale),
        lineHeight = 19.sp.scaled(disableAutoScale, fontScale),
        letterSpacing = 0.6.sp.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextC70(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    isUpperCase: Boolean = true,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.W500,
) {
    Text(
        text = if (isUpperCase) text.uppercase(Locale.getDefault()) else text,
        color = MaterialTheme.theme.colors.primaryText02,
        fontFamily = FontFamily.SansSerif,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = 14.sp.scaled(disableAutoScale, fontScale),
        letterSpacing = 0.6.sp.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextC70(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    disableAutoScale: Boolean = false,
    fontScale: Float = 1f,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.W500,
) {
    Text(
        text = text,
        color = MaterialTheme.theme.colors.primaryText02,
        fontFamily = FontFamily.SansSerif,
        fontSize = fontSize.scaled(disableAutoScale, fontScale),
        lineHeight = 14.sp.scaled(disableAutoScale, fontScale),
        letterSpacing = 0.6.sp.scaled(disableAutoScale, fontScale),
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun TextUnit.scaled(disabled: Boolean, fontScale: Float) = (if (disabled) value.nonScaledSp else this) * fontScale

@Preview(name = "Light")
@Composable
private fun TextStylesLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        TextStylesPreview()
    }
}

@Preview(name = "Dark")
@Composable
private fun TextStylesDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TextStylesPreview()
    }
}

@Composable
private fun TextStylesPreview() {
    Column {
        val modifier = Modifier.padding(vertical = 2.dp)
        TextH10("TextH10 - 31 / 700", modifier = modifier)
        TextH20("TextH20 - 22 / 700", modifier = modifier)
        TextH30("TextH30 - 18 / 600", modifier = modifier)
        TextP30("TextP30 - 18 / 500", modifier = modifier)
        TextH40("TextH40 - 15 / 500", modifier = modifier)
        TextP40("TextP40 - 16 / 400", modifier = modifier)
        TextH50("TextH50 - 14 / 500", modifier = modifier)
        TextP50("TextP50 - 14 / 400", modifier = modifier)
        TextP60("TextP60 - 13 / 400", modifier = modifier)
        TextH70("TextH70 - 12 / 500", modifier = modifier)
        TextC50("TextC50 - 13 / 700", modifier = modifier)
        TextC70("TextC70 - 12 / 500", modifier = modifier)
    }
}
