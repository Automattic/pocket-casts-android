package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Locale

private val Float.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

private val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

@Composable
fun TextH10(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        color = color,
        fontSize = 31.sp,
        lineHeight = 37.sp,
        fontWeight = FontWeight.W700,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun TextH20(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    disableScale: Boolean = false,
    fontSize: TextUnit = 22.sp,
    lineHeight: TextUnit = 30.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = if (disableScale) fontSize.value.nonScaledSp else fontSize,
        lineHeight = if (disableScale) lineHeight.value.nonScaledSp else lineHeight.value.sp,
        fontWeight = FontWeight.W700,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun TextH30(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    disableScale: Boolean = false,
    fontSize: TextUnit? = null,
    lineHeight: TextUnit = 21.sp,
) {
    val fontSizeUpdated = fontSize ?: 18.sp
    Text(
        text = text,
        color = color,
        fontSize = if (disableScale) fontSizeUpdated.value.nonScaledSp else fontSizeUpdated,
        lineHeight = if (disableScale) lineHeight.value.nonScaledSp else lineHeight.value.sp,
        textAlign = textAlign,
        fontWeight = fontWeight ?: FontWeight.W600,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
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
) {
    Text(
        text = text,
        color = color,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        modifier = modifier
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
    disableScale: Boolean = false
) {
    Text(
        text = text,
        color = color,
        fontSize = if (disableScale) 15.nonScaledSp else 15.sp,
        fontWeight = fontWeight,
        lineHeight = if (disableScale) 21.nonScaledSp else 21.sp,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun TextP40(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    disableScale: Boolean = false,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 22.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = if (disableScale) fontSize.value.nonScaledSp else fontSize,
        lineHeight = if (disableScale) lineHeight.value.nonScaledSp else lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun TextH50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.W500,
    lineHeight: TextUnit = 20.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
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
) {
    TextP50(
        text = AnnotatedString(text),
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        style = style,
        textAlign = textAlign,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
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
) {
    Text(
        text = text,
        color = color ?: MaterialTheme.theme.colors.primaryText01,
        fontSize = 14.sp,
        lineHeight = lineHeight ?: 20.sp,
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        style = style ?: LocalTextStyle.current,
        textAlign = textAlign,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

val textH60FontSize = 12.sp
@Composable
fun TextH60(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        color = color,
        fontSize = textH60FontSize,
        fontWeight = FontWeight.W600,
        lineHeight = 14.sp,
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
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun TextH70(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    fontWeight: FontWeight = FontWeight.W500,
    maxLines: Int = Int.MAX_VALUE,
    disableScale: Boolean = false
) {
    Text(
        text = text,
        color = color,
        fontSize = if (disableScale) 12.nonScaledSp else 12.sp,
        fontWeight = fontWeight,
        lineHeight = if (disableScale) 14.nonScaledSp else 14.sp,
        letterSpacing = if (disableScale) .25f.nonScaledSp else .25.sp,
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
    maxLines: Int = Int.MAX_VALUE
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
        modifier = modifier
    )
}

@Composable
fun TextC70(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        color = MaterialTheme.theme.colors.primaryText02,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun TextStylesLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        TextStylesPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextStylesDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        TextStylesPreview()
    }
}

@Composable
private fun TextStylesPreview() {
    Column {
        TextH10("H10")
        TextH20("H20")
        TextH30("H30")
        TextP30("P30")
        TextH40("H40")
        TextP40("P40")
        TextH50("H50")
        TextP50("P50")
        TextP60("P60")
        TextH70("H70")
        TextC50("C50")
        TextC70("C70")
    }
}
