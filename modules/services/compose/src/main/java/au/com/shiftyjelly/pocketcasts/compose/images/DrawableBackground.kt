package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.annotation.DrawableRes
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Suppress("ktlint:compose:modifier-composed-check")
fun Modifier.drawableBackground(@DrawableRes drawableId: Int) = composed {
    val context = LocalContext.current
    val drawable = remember(context, drawableId) { ContextCompat.getDrawable(context, drawableId)?.mutate() }
    drawBehind {
        drawable?.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        drawable?.draw(drawContext.canvas.nativeCanvas)
    }
}
