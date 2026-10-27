package au.com.shiftyjelly.pocketcasts.compose.text

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberThemeAttributeColor(@AttrRes colorAttr: Int): Color {
    val context = LocalContext.current
    return remember(context, colorAttr) {
        context.styledColor(styleRes = 0, attr = colorAttr)
    }
}

@Composable
fun rememberTextAppearanceColor(@AttrRes textAppearanceAttr: Int): Color {
    val context = LocalContext.current
    return remember(context, textAppearanceAttr) {
        val value = TypedValue()
        context.theme.resolveAttribute(textAppearanceAttr, value, true)
        context.styledColor(styleRes = value.resourceId, attr = android.R.attr.textColor)
    }
}

private fun Context.styledColor(@StyleRes styleRes: Int, @AttrRes attr: Int): Color {
    val typedArray = obtainStyledAttributes(styleRes, intArrayOf(attr))
    try {
        return Color(typedArray.getColor(0, 0))
    } finally {
        typedArray.recycle()
    }
}
