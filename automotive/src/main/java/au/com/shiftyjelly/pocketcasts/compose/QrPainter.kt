package au.com.shiftyjelly.pocketcasts.compose

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

private sealed class QrState {
    object Loading : QrState()
    data class Success(val bitmap: Bitmap) : QrState()
    object Error : QrState()
}

@Composable
fun rememberQrPainter(
    content: String,
    size: Dp = 300.dp,
): Painter {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    var qrState by remember(content, sizePx) {
        mutableStateOf<QrState>(QrState.Loading)
    }

    LaunchedEffect(content, sizePx) {
        qrState = QrState.Loading

        launch(Dispatchers.IO) {
            try {
                if (content.isBlank()) {
                    qrState = QrState.Error
                    return@launch
                }

                val qrCodeWriter = QRCodeWriter()
                val encodeHints = mutableMapOf<EncodeHintType, Any?>().apply {
                    this[EncodeHintType.MARGIN] = 0
                    this[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
                }

                val bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    encodeHints,
                )

                val newBitmap = createBitmap(bitMatrix.width, bitMatrix.height)

                for (x in 0 until bitMatrix.width) {
                    for (y in 0 until bitMatrix.height) {
                        val shouldColorPixel = bitMatrix[x, y]
                        val color = if (shouldColorPixel) Color.WHITE else Color.BLACK
                        newBitmap[x, y] = color
                    }
                }

                qrState = QrState.Success(newBitmap)
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to generate QR code")
                qrState = QrState.Error
            }
        }
    }

    // Recycle the bitmap
    DisposableEffect(content, sizePx) {
        onDispose {
            val currentState = qrState
            if (currentState is QrState.Success) {
                try {
                    if (!currentState.bitmap.isRecycled) {
                        currentState.bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to recycle bitmap")
                }
            }
        }
    }

    return remember(qrState) {
        when (val state = qrState) {
            is QrState.Loading -> {
                ColorPainter(androidx.compose.ui.graphics.Color.Transparent)
            }
            is QrState.Success -> {
                BitmapPainter(state.bitmap.asImageBitmap())
            }
            is QrState.Error -> {
                ColorPainter(androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}
