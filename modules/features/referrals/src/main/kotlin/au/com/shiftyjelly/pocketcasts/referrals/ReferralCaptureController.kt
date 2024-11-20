package au.com.shiftyjelly.pocketcasts.referrals

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer.TAG_CRASH
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberReferralCaptureController(): ReferralCaptureController {
    val context = LocalContext.current
    val referralCardController = rememberCaptureController()

    return remember {
        object : ReferralCaptureController {
            override fun captureController() = referralCardController

            override suspend fun capture() = runCatching {
                val controller = captureController()
                val capturedBitmap = controller.captureAsync().await()
                val file = File(context.cacheDir, "pocket-casts-plus-guest-pass.png")
                file.outputStream().use { stream ->
                    withContext(Dispatchers.IO) {
                        capturedBitmap.asAndroidBitmap()
                            .copy(Bitmap.Config.ARGB_8888, false)
                            .compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
                file
            }.onFailure { LogBuffer.e(TAG_CRASH, it, "Failed to create a screenshot") }
                .getOrNull()
        }
    }
}

internal interface ReferralCaptureController {
    fun captureController(): CaptureController

    suspend fun capture(): File?

    companion object {
        @Composable
        fun preview() = object : ReferralCaptureController {
            private val controller = rememberCaptureController()

            override fun captureController() = controller

            override suspend fun capture(): File? {
                return null
            }
        }
    }
}
