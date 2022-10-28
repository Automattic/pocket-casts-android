package au.com.shiftyjelly.pocketcasts.endofyear.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap

/* Returns a callback to get latest composable bitmap.
Credits: https://rb.gy/g5vuez */
@Composable
fun snapShot(
    content: @Composable () -> Unit,
): () -> Bitmap {
    val context = LocalContext.current
    val composeView = remember { ComposeView(context) }

    fun onCaptureBitmap(): Bitmap {
        return composeView.drawToBitmap()
    }

    AndroidView(
        factory = {
            composeView.apply {
                setContent {
                    content.invoke()
                }
            }
        }
    )

    return ::onCaptureBitmap
}
