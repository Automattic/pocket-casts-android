package au.com.shiftyjelly.pocketcasts.endofyear

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import au.com.shiftyjelly.pocketcasts.endofyear.ui.backgroundColor
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer.TAG_CRASH
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun rememberStoryCaptureController(): StoryCaptureController {
    val context = LocalContext.current
    val cover = rememberCaptureController()
    val numOfShows = rememberCaptureController()
    val topShow = rememberCaptureController()
    val topShows = rememberCaptureController()
    val ratings = rememberCaptureController()
    val totalTime = rememberCaptureController()
    val longestEpisode = rememberCaptureController()
    val plusInterstitial = rememberCaptureController()
    val yearVsYear = rememberCaptureController()
    val completionRate = rememberCaptureController()
    val ending = rememberCaptureController()

    return remember {
        object : StoryCaptureController {
            private var _isSharing by mutableStateOf(false)
            override val isSharing get() = _isSharing

            private val buttonHeights = mutableMapOf<Class<Story>, Int>()

            override fun updateButtonHeightPx(story: Story, height: Int) {
                buttonHeights += story.javaClass to height
            }

            override fun captureController(story: Story): CaptureController = when (story) {
                is Story.Cover -> cover
                is Story.NumberOfShows -> numOfShows
                is Story.TopShow -> topShow
                is Story.TopShows -> topShows
                is Story.Ratings -> ratings
                is Story.TotalTime -> totalTime
                is Story.LongestEpisode -> longestEpisode
                is Story.PlusInterstitial -> plusInterstitial
                is Story.YearVsYear -> yearVsYear
                is Story.CompletionRate -> completionRate
                is Story.Ending -> ending
            }

            override suspend fun capture(story: Story): File? {
                val buttonHeightPx = buttonHeights[story.javaClass]
                if (buttonHeightPx == null) {
                    return null
                }

                _isSharing = true
                delay(50) // A small delay to settle stories animations before capturing a screenshot
                val controller = captureController(story)
                val file = runCatching {
                    val bitmap: Bitmap = withContext(Dispatchers.Default) {
                        val background = controller.captureAsync().await()
                            .asAndroidBitmap()
                            .copy(Bitmap.Config.ARGB_8888, false)

                        val logoHeight = (buttonHeightPx * 0.5f).roundToInt()
                        val logoWidth = ((logoHeight * 153).toDouble() / 38).roundToInt()
                        val pcLogo = AppCompatResources.getDrawable(context, IR.drawable.pc_logo_pill)!!
                            .toBitmap()
                            .scale(width = logoWidth, height = logoHeight)

                        Bitmap.createBitmap(background.width, background.height, Bitmap.Config.ARGB_8888).applyCanvas {
                            // Draw captured bitmap
                            drawBitmap(background, 0f, 0f, null)
                            // Hide bottom button behind an empty rect
                            val paint = Paint().apply {
                                isDither = true
                                color = story.backgroundColor.toArgb()
                            }
                            drawRect(
                                0f,
                                height - buttonHeightPx.toFloat(),
                                width.toFloat(),
                                height.toFloat(),
                                paint,
                            )
                            // Draw PC logo
                            drawBitmap(
                                pcLogo,
                                (width - pcLogo.width).toFloat() / 2,
                                (height - buttonHeightPx + (buttonHeightPx - pcLogo.height) / 2).toFloat(),
                                null,
                            )
                        }
                    }
                    withContext(Dispatchers.IO) {
                        val file = File(context.cacheDir, "pocket-casts-playback-screenshot.png")
                        file.outputStream().use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        file
                    }
                }.onFailure { LogBuffer.e(TAG_CRASH, it, "Failed to create a screenshot") }
                    .getOrNull()
                _isSharing = false
                return file
            }
        }
    }
}

internal interface StoryCaptureController {
    val isSharing: Boolean

    fun updateButtonHeightPx(story: Story, height: Int)
    fun captureController(story: Story): CaptureController
    suspend fun capture(story: Story): File?

    companion object {
        @Composable
        fun preview() = object : StoryCaptureController {
            private val controller = rememberCaptureController()

            override val isSharing = false

            override fun captureController(story: Story) = controller

            override fun updateButtonHeightPx(story: Story, height: Int) = Unit

            override suspend fun capture(story: Story): File? {
                return null
            }
        }
    }
}
