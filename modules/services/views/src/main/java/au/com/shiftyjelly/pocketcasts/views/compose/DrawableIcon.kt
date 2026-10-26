package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.ColorStateList
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun DrawableIcon(
    @DrawableRes drawableId: Int,
    tint: Color?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable = remember(drawableId, context) { AppCompatResources.getDrawable(context, drawableId)?.mutate() }
    Spacer(
        modifier.drawBehind {
            if (drawable != null) {
                drawable.setTintList(tint?.let { ColorStateList.valueOf(it.toArgb()) })
                drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawIntoCanvas { drawable.draw(it.nativeCanvas) }
            }
        },
    )
}
