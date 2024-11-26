package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.airbnb.android.showkase.annotation.ShowkaseComposable
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
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        lineHeight = (if (disableAutoScale) lineHeight.value.nonScaledSp else lineHeight) * fontScale,
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
    disableAutoScale: Boolean = false,
    fontSize: TextUnit = 22.sp,
    lineHeight: TextUnit = 30.sp,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        lineHeight = (if (disableAutoScale) lineHeight.value.nonScaledSp else lineHeight.value.sp) * fontScale,
        fontWeight = FontWeight.W700,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
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
    style: TextStyle = TextStyle(),
    fontScale: Float = 1f,
) {
    val fontSizeUpdated = fontSize ?: 18.sp
    Text(
        text = text,
        color = color,
        fontFamily = fontFamily,
        fontSize = (if (disableAutoScale) fontSizeUpdated.value.nonScaledSp else fontSizeUpdated) * fontScale,
        lineHeight = (if (disableAutoScale) lineHeight.value.nonScaledSp else lineHeight.value.sp) * fontScale,
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
        fontSize = (if (disableAutoScale) 18.nonScaledSp else 18.sp) * fontScale,
        lineHeight = 24.sp,
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
) {
    Text(
        text = text,
        color = color,
        fontSize = (if (disableAutoScale) 15.nonScaledSp else 15.sp) * fontScale,
        fontWeight = fontWeight,
        lineHeight = (if (disableAutoScale) 21.nonScaledSp else 21.sp) * fontScale,
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
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 22.sp,
    fontScale: Float = 1f,
) {
    Text(
        text = text,
        color = color,
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        lineHeight = (if (disableAutoScale) lineHeight.value.nonScaledSp else lineHeight) * fontScale,
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
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        fontWeight = fontWeight,
        lineHeight = (if (disableAutoScale) lineHeight.value.nonScaledSp else lineHeight) * fontScale,
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
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        lineHeight = (if (disableAutoScale) lineHeightUpdated.value.nonScaledSp else lineHeightUpdated.value.sp) * fontScale,
        letterSpacing = (if (disableAutoScale) letterSpacingUpdated.value.nonScaledSp else letterSpacingUpdated.value.sp) * fontScale,
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
        fontSize = (if (disableAutoScale) fontSize.value.nonScaledSp else fontSize) * fontScale,
        fontWeight = fontWeight,
        lineHeight = (if (disableAutoScale) 14.nonScaledSp else 14.sp) * fontScale,
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
) {
    val fontSizeUpdated = fontSize ?: 13.sp
    Text(
        text = text,
        color = color,
        fontSize = (if (disableAutoScale) fontSizeUpdated.value.nonScaledSp else fontSizeUpdated) * fontScale,
        lineHeight = (if (disableAutoScale) 15.nonScaledSp else 15.sp) * fontScale,
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
        fontSize = (if (disableAutoScale) fontSizeUpdated.value.nonScaledSp else fontSizeUpdated) * fontScale,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        lineHeight = (if (disableAutoScale) 14.nonScaledSp else 14.sp) * fontScale,
        letterSpacing = if (disableAutoScale) .25f.nonScaledSp else .25.sp,
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
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        color = MaterialTheme.theme.colors.primaryText02,
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.W700,
        lineHeight = 19.sp,
        letterSpacing = 0.6.sp,
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
) {
    Text(
        text = if (isUpperCase) text.uppercase(Locale.getDefault()) else text,
        color = MaterialTheme.theme.colors.primaryText02,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@ShowkaseComposable(name = "Text", group = "Text", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun TextStylesLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        TextStylesPreview()
    }
}

@ShowkaseComposable(name = "Text", group = "Text", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun TextStylesDarkPreview() {
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
