package au.com.shiftyjelly.pocketcasts.settings.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

class WebViewScreenshotCapture @Inject constructor() {
    suspend fun captureScreenshot(webView: WebView, activity: Activity) = captureToFile(webView) { activity.window }

    private suspend fun captureToFile(webView: WebView, windowProvider: () -> Window) = runCatching {
        val bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            drawToCanvas(webView)
        } else {
            usePixelCopy(webView, windowProvider())
        }
        writeToFile(webView.context, bitmap)
    }.onSuccess {
        Timber.d("Captured bitmap using PixelCopy API")
    }.onFailure {
        Timber.d("Failed to capture bitmap using PixelCopy API: $it")
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun usePixelCopy(webView: WebView, window: Window) = suspendCancellableCoroutine {
        val location = IntArray(2)
        webView.getLocationInWindow(location)
        val x = location[0]
        val y = location[1]

        val width = webView.width
        val height = webView.height

        if (width == 0 || height == 0) {
            it.resumeWithException(IllegalStateException("Failed to determine webview size"))
        }

        val bitmap = createBitmap(width, height)

        try {
            PixelCopy.request(
                window,
                Rect(x, y, x + width, y + height),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        it.resume(bitmap)
                    } else {
                        it.resumeWithException(IllegalStateException("Failed to capture bitmap using PixelCopy API"))
                    }
                },
                android.os.Handler(Looper.getMainLooper()),
            )
        } catch (e: IllegalArgumentException) {
            it.resumeWithException(e)
        }
    }

    private fun drawToCanvas(webView: WebView): Bitmap {
        val width = webView.width
        val height = webView.height
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        return bitmap
    }

    private suspend fun writeToFile(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "pocket-casts-eoy.png")
        file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        file
    }
}
