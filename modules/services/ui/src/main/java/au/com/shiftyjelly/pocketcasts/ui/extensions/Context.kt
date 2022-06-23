package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat

fun Context.getThemeColor(@AttrRes themeAttrId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(themeAttrId, typedValue, true)
    return typedValue.data
}

fun Context.getComposeThemeColor(@AttrRes themeAttrId: Int): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(getThemeColor(themeAttrId))
}

fun Context.getThemeDrawable(themeDrawableId: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(themeDrawableId, tv, true)
    return tv.resourceId
}

fun Context.getThemeTintedDrawable(drawableId: Int, themeColorId: Int): Drawable? {
    return getTintedDrawable(drawableId, getThemeColor(themeColorId))
}

fun Context.getTintedDrawable(drawableId: Int, @ColorInt tintColor: Int): Drawable? {
    val drawable = AppCompatResources.getDrawable(this, drawableId) ?: return null
    drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(tintColor, BlendModeCompat.SRC_ATOP)
    return drawable
}

fun Context.getAttrTextStyleColor(themeAttrId: Int): Int {
    val typedStyle = TypedValue()
    theme.resolveAttribute(themeAttrId, typedStyle, true)

    val attrs = arrayOf(android.R.attr.textColor).toIntArray()
    val typedArray = obtainStyledAttributes(typedStyle.resourceId, attrs)
    val textColor = typedArray.getColor(0, Color.RED) // Red so you can see when its broken
    typedArray.recycle()
    return textColor
}
