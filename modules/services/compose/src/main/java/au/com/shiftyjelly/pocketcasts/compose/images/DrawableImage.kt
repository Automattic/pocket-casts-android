package au.com.shiftyjelly.pocketcasts.compose.images

import android.content.res.ColorStateList
import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.core.content.ContextCompat

@Composable
fun DrawableImage(
    @DrawableRes drawableId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val context = LocalContext.current
    val drawable = remember(drawableId, context) { ContextCompat.getDrawable(context, drawableId)?.mutate() }
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Spacer(
        modifier = modifier
            .layout { measurable, constraints ->
                val width = constraints.constrainWidth(drawable?.intrinsicWidth ?: 0)
                val height = constraints.constrainHeight(drawable?.intrinsicHeight ?: 0)
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .then(semantics)
            .drawBehind {
                if (drawable == null) return@drawBehind
                drawable.setTintList(tint?.let { ColorStateList.valueOf(it.toArgb()) })
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                val matrix = Matrix().apply {
                    setRectToRect(
                        RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat()),
                        RectF(0f, 0f, size.width, size.height),
                        Matrix.ScaleToFit.CENTER,
                    )
                }
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val count = nativeCanvas.save()
                    nativeCanvas.concat(matrix)
                    drawable.draw(nativeCanvas)
                    nativeCanvas.restoreToCount(count)
                }
            },
    )
}
