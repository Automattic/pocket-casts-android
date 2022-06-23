package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.Context
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

/**
 * Returns the color value, instead of just the color index.
 */
@ColorInt
fun Folder.getColor(context: Context): Int {
    val colorAttr = Theme.folderColors.getOrElse(color) { Theme.folderColors.first() }
    return context.getThemeColor(colorAttr)
}
