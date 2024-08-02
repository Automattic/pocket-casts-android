package au.com.shiftyjelly.pocketcasts.sharing.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.scale
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType.Horiozntal
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType.Square
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType.Vertical
import dev.shreyaspatil.capturable.controller.CaptureController
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class VideoBackgroundController(
    private val context: Context,
    private val shareColors: ShareColors,
) {
    val verticalCardController = CaptureController()
    val horizontalCardController = CaptureController()
    val squareCardController = CaptureController()

    @OptIn(ExperimentalComposeApi::class)
    suspend fun create(type: CardType) = runCatching {
        val controller = when (type) {
            Vertical -> verticalCardController
            Horiozntal -> horizontalCardController
            Square -> squareCardController
        }
        val capturedBitmap = controller.captureAsync().await()
        val backgroundBitmap = withContext(Dispatchers.Default) {
            val scaledBitmap = capturedBitmap.asAndroidBitmap()
                .scale(width = BITMAP_WIDTH, height = (capturedBitmap.height.toDouble() * BITMAP_WIDTH / capturedBitmap.width).roundToInt())
                .copy(Bitmap.Config.ARGB_8888, false)
            createBackgroundBitmap(scaledBitmap, type)
        }
        val file = File(context.cacheDir, "share-background-image.png")
        file.outputStream().use { stream ->
            withContext(Dispatchers.IO) {
                backgroundBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        file
    }

    private fun createBackgroundBitmap(
        foregroundBitmap: Bitmap,
        type: CardType,
    ) = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888).applyCanvas {
        drawRect(0f, 0f, BITMAP_HEIGHT.toFloat(), BITMAP_HEIGHT.toFloat(), type.backgroundPaint)
        drawBitmap(foregroundBitmap, (BITMAP_WIDTH.toFloat() - foregroundBitmap.width) / 2, (BITMAP_HEIGHT.toFloat() - foregroundBitmap.height) / 2, null)
    }

    private val CardType.backgroundPaint get() = when (this) {
        Vertical -> Paint().apply {
            isDither = true
            shader = LinearGradient(0f, 0f, 0f, BITMAP_HEIGHT.toFloat(), shareColors.cardBottom.toArgb(), shareColors.cardTop.toArgb(), Shader.TileMode.CLAMP)
        }
        Horiozntal, Square -> Paint().apply {
            isDither = true
            color = shareColors.background.toArgb()
        }
    }

    private companion object {
        const val BITMAP_WIDTH = 1080
        const val BITMAP_HEIGHT = 1920
    }
}
