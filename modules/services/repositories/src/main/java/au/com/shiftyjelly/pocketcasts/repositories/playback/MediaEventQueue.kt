package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MediaEventQueue(
    private val scopeProvider: () -> CoroutineScope,
    // Deadlines are measured against elapsed realtime so the window still closes while the device is suspended.
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private var singleTapJob: SingleTapJob? = null
    private var multiTapWindowEndsAt = Long.MIN_VALUE
    private val stateMutex = Mutex()

    private val scope: CoroutineScope get() = scopeProvider()

    private val isMultiTapWindowOpen get() = elapsedRealtime() <= multiTapWindowEndsAt

    suspend fun consumeEvent(
        event: MediaEvent,
        onImmediateSingleTap: (() -> Unit)? = null,
    ) = when (event) {
        MediaEvent.SingleTap -> handleSingleTapEvent(onImmediateSingleTap)
        MediaEvent.DoubleTap, MediaEvent.TripleTap -> handleMultiTapEvent(event)
    }

    private suspend fun handleSingleTapEvent(onImmediateSingleTap: (() -> Unit)?): MediaEvent? {
        val newSingleTapJob = stateMutex.withLock {
            val currentSingleTapJob = singleTapJob
            when {
                // Pixel Buds (and possibly other headphones) trigger KEYCODE_MEDIA_PLAY
                // after KEYCODE_MEDIA_NEXT or KEYCODE_MEDIA_PREVIOUS.
                // We need to ignore it so the single tap action isn't triggered in such cases.
                isMultiTapWindowOpen -> null

                currentSingleTapJob?.isActive == true -> {
                    currentSingleTapJob.incrementTaps()
                    null
                }

                else -> SingleTapJob(scope).also { singleTapJob = it }
            }
        } ?: return null

        try {
            onImmediateSingleTap?.invoke()
        } catch (e: Exception) {
            stateMutex.withLock {
                if (singleTapJob === newSingleTapJob) {
                    singleTapJob = null
                    newSingleTapJob.cancel()
                }
            }
            throw e
        }
        newSingleTapJob.await()
        return stateMutex.withLock {
            // The immediate callback owns a resolved SingleTap. Follow-up taps still
            // return their DoubleTap or TripleTap action after the window closes.
            newSingleTapJob.event().takeUnless {
                it == MediaEvent.SingleTap && onImmediateSingleTap != null
            }
        }
    }

    private suspend fun handleMultiTapEvent(event: MediaEvent): MediaEvent = stateMutex.withLock {
        multiTapWindowEndsAt = elapsedRealtime() + MULTI_TAP_WINDOW_MS
        event
    }

    private class SingleTapJob(
        scope: CoroutineScope,
    ) {
        private var counter: Int = 1

        private val job = scope.launch { delay(600) }

        val isActive get() = job.isActive

        suspend fun await() = job.join()

        fun cancel() = job.cancel()

        fun incrementTaps() {
            counter++
        }

        fun event() = when (counter) {
            1 -> MediaEvent.SingleTap
            2 -> MediaEvent.DoubleTap
            else -> MediaEvent.TripleTap
        }
    }

    private companion object {
        const val MULTI_TAP_WINDOW_MS = 250L
    }
}

internal enum class MediaEvent {
    SingleTap,
    DoubleTap,
    TripleTap,
}
