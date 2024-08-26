package au.com.shiftyjelly.pocketcasts.reimagine.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.scale
import au.com.shiftyjelly.pocketcasts.sharing.CardType.Horizontal
import au.com.shiftyjelly.pocketcasts.sharing.CardType.Square
import au.com.shiftyjelly.pocketcasts.sharing.CardType.Vertical
import au.com.shiftyjelly.pocketcasts.sharing.VisualCardType
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface BackgroundAssetController {
    fun captureController(type: VisualCardType): CaptureController

    suspend fun capture(type: VisualCardType): Result<File>

    companion object {
        private const val BITMAP_WIDTH = 1080
        private const val BITMAP_HEIGHT = 1920

        fun create(context: Context, shareColors: ShareColors) = object : BackgroundAssetController {
            private val verticalCardController = CaptureController()
            private val horizontalCardController = CaptureController()
            private val squareCardController = CaptureController()

            override fun captureController(type: VisualCardType) = when (type) {
                Vertical -> verticalCardController
                Horizontal -> horizontalCardController
                Square -> squareCardController
            }

            @OptIn(ExperimentalComposeApi::class)
            override suspend fun capture(type: VisualCardType) = runCatching {
                val controller = captureController(type)
                val capturedBitmap = controller.captureAsync().await()
                val backgroundBitmap = withContext(Dispatchers.Default) {
                    val scaledWidth = type.targetWidth
                    val scaledBitmap = capturedBitmap.asAndroidBitmap()
                        .scale(width = scaledWidth, height = (capturedBitmap.height.toDouble() * scaledWidth / capturedBitmap.width).roundToInt())
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
                type: VisualCardType,
            ) = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888).applyCanvas {
                drawRect(0f, 0f, BITMAP_HEIGHT.toFloat(), BITMAP_HEIGHT.toFloat(), type.backgroundPaint)
                drawBitmap(foregroundBitmap, (BITMAP_WIDTH.toFloat() - foregroundBitmap.width) / 2, (BITMAP_HEIGHT.toFloat() - foregroundBitmap.height) / 2, null)
            }

            private val VisualCardType.targetWidth get() = when (this) {
                Vertical -> BITMAP_WIDTH
                Horizontal, Square -> (BITMAP_WIDTH * 0.9).roundToInt()
            }

            private val VisualCardType.backgroundPaint get() = when (this) {
                Vertical -> Paint().apply {
                    isDither = true
                    shader = LinearGradient(0f, 0f, 0f, BITMAP_HEIGHT.toFloat(), shareColors.cardBottom.toArgb(), shareColors.cardTop.toArgb(), Shader.TileMode.CLAMP)
                }
                Horizontal, Square -> Paint().apply {
                    isDither = true
                    color = shareColors.background.toArgb()
                }
            }
        }

        @Composable
        fun preview() = object : BackgroundAssetController {
            private val controller = rememberCaptureController()

            override fun captureController(type: VisualCardType) = controller

            override suspend fun capture(type: VisualCardType): Result<File> {
                throw UnsupportedOperationException("Preview controller")
            }
        }
    }
}
