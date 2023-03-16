package au.com.shiftyjelly.pocketcasts.helper

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun rememberQrPainter(
    content: String,
    size: Dp = 300.dp
): BitmapPainter {

    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    var bitmap by remember(content) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(bitmap) {
        if (bitmap != null) return@LaunchedEffect

        launch(Dispatchers.IO) {
            val qrCodeWriter = QRCodeWriter()

            val encodeHints = mutableMapOf<EncodeHintType, Any?>()
                .apply {
                    this[EncodeHintType.MARGIN] = 0
                    this[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
                }

            val bitMatrix = try {
                qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, encodeHints)
            } catch (ex: WriterException) {
                Timber.e(ex, "Failed to encode QR code")
                null
            }

            val matrixWidth = bitMatrix?.width ?: sizePx
            val matrixHeight = bitMatrix?.height ?: sizePx

            val newBitmap = Bitmap.createBitmap(
                bitMatrix?.width ?: sizePx,
                bitMatrix?.height ?: sizePx,
                Bitmap.Config.ARGB_8888
            )

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    val shouldColorPixel = bitMatrix?.get(x, y) ?: false
                    val color = if (shouldColorPixel) Color.WHITE else Color.BLACK

                    newBitmap.setPixel(x, y, color)
                }
            }

            bitmap = newBitmap
        }
    }

    return remember(bitmap) {
        val currentBitmap = bitmap
            ?: Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(Color.TRANSPARENT) }
        BitmapPainter(currentBitmap.asImageBitmap())
    }
}
