package au.com.shiftyjelly.pocketcasts.endofyear.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap

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

    return { composeView.drawToBitmap() }
}
