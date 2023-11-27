package au.com.shiftyjelly.pocketcasts.endofyear.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewAspectRatioForTablet
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.deviceAspectRatio
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val AppLogoWidthInDp = 130
private const val AppLogoHeightInDp = 26
private const val AppLogoPaddingBottomInDp = 40

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
    // Draw story view
    view.draw(canvas)
    // Draw app logo
    canvas.drawAppLogo(view.context)

    return bitmap
}

private fun Canvas.drawAppLogo(
    context: Context,
) {
    val appLogoBitmap = (
        ContextCompat
            .getDrawable(context, IR.drawable.ic_logo_title_hor_light) as? VectorDrawable
        )
        ?.toBitmap(
            width = AppLogoWidthInDp.dpToPx(context),
            height = AppLogoHeightInDp.dpToPx(context),
        )
    appLogoBitmap?.let {
        drawBitmap(
            it,
            (this.width - appLogoBitmap.width) / 2f,
            (this.height - (appLogoBitmap.height + AppLogoPaddingBottomInDp.dpToPx(context))).toFloat(),
            null,
        )
    }
}

private fun getAspectRatioForBitmap(context: Context) =
    if (Util.isTablet(context)) {
        StoriesViewAspectRatioForTablet
    } else {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        wm?.deviceAspectRatio()
    } ?: StoriesViewAspectRatioForTablet
