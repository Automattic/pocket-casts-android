package au.com.shiftyjelly.pocketcasts.endofyear.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewAspectRatioForTablet
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.deviceAspectRatio
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx

/* Returns a callback to get bitmap for the passed composable.
The composable is converted to ComposeView and laid out into AndroidView otherwise an illegal state exception is thrown:
View needs to be laid out before calling drawToBitmap()
Credits: https://rb.gy/g5vuez */
@Composable
fun convertibleToBitmap(
    content: @Composable () -> Unit,
): () -> Bitmap {
    val context = LocalContext.current
    val composeView = remember { ComposeView(context) }

    AndroidView(
        factory = {
            composeView.apply {
                setContent {
                    content.invoke()
                }
            }
        }
    )

    return {
        val height = composeView.width * getAspectRatioForBitmap(context)
        val availableHeight = height - 50.dpToPx(context) // Reduce approx share button height
        createBitmapFromView(
            view = composeView,
            width = composeView.width,
            height = availableHeight.toInt()
        )
    }
}

fun createBitmapFromView(view: View, width: Int, height: Int): Bitmap {
    view.layoutParams = LinearLayoutCompat.LayoutParams(
        LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
        LinearLayoutCompat.LayoutParams.WRAP_CONTENT
    )

    view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )

    view.layout(0, 0, width, height)

    val canvas = Canvas()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    canvas.setBitmap(bitmap)
    view.draw(canvas)

    return bitmap
}

private fun getAspectRatioForBitmap(context: Context) =
    if (Util.isTablet(context)) {
        StoriesViewAspectRatioForTablet
    } else {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        wm?.deviceAspectRatio()
    } ?: StoriesViewAspectRatioForTablet
