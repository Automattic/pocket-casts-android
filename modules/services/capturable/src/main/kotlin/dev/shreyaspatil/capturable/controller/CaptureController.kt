/*
* MIT License
*
* Copyright (c) 2022 Shreyas Patil
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*
*/
package dev.shreyaspatil.capturable.controller

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Author: Shreyas Patil
 * Modifications: Michał Sikora
 */

/**
 * Controller for capturing [Composable] content.
 * @see dev.shreyaspatil.capturable.Capturable for implementation details.
 *
 */
class CaptureController(internal val graphicsLayer: GraphicsLayer) {

    /**
     * Medium for providing capture requests
     *
     * Earlier, we were using `MutableSharedFlow` here but it was incapable of serving requests
     * which are created as soon as composition starts because this flow was collected later
     * underneath. So Channel with UNLIMITED capacity just works here and solves the issue as well.
     * See issue: https://github.com/PatilShreyas/Capturable/issues/202
     */
    @Suppress("ktlint")
    private val _captureRequests = Channel<CaptureRequest>(capacity = Channel.UNLIMITED)
    internal val captureRequests = _captureRequests.receiveAsFlow()

    /**
     * Creates and requests for a Bitmap capture with specified [config] and returns
     * an [ImageBitmap] asynchronously.
     *
     * This method is safe to be called from the "main" thread directly.
     *
     * Make sure to call this method as a part of callback function and not as a part of the
     * [Composable] function itself.
     *
     * @param config Bitmap config of the desired bitmap. Defaults to [Bitmap.Config.ARGB_8888]
     */
    fun captureAsync(): Deferred<ImageBitmap> {
        val deferredImageBitmap = CompletableDeferred<ImageBitmap>()
        return deferredImageBitmap.also {
            _captureRequests.trySend(CaptureRequest(imageBitmapDeferred = it))
        }
    }

    /**
     * Holds information of capture request
     */
    internal class CaptureRequest(val imageBitmapDeferred: CompletableDeferred<ImageBitmap>)
}

/**
 * Creates [CaptureController] and remembers it.
 */
@Composable
fun rememberCaptureController(): CaptureController {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { CaptureController(graphicsLayer) }
}
