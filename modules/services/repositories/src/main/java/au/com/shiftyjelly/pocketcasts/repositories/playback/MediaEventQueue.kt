package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MediaEventQueue(
    private val scopeProvider: () -> CoroutineScope,
) {
    private var singleTapJob: SingleTapJob? = null
    private var multiTapJob: Job? = null
    private val stateMutex = Mutex()

    private val scope: CoroutineScope get() = scopeProvider()

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
                multiTapJob?.isActive == true -> null

                currentSingleTapJob?.isActive == true -> {
                    currentSingleTapJob.incrementTaps()
                    null
                }

                else -> SingleTapJob(scope).also { singleTapJob = it }
            }
        } ?: return null

        onImmediateSingleTap?.invoke()
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
        val currentJob = multiTapJob
        multiTapJob = scope.launch { delay(250) }
        currentJob?.cancel()
        event
    }

    private class SingleTapJob(
        scope: CoroutineScope,
    ) {
        private var counter: Int = 1

        private val job = scope.launch { delay(600) }

        val isActive get() = job.isActive

        suspend fun await() = job.join()

        fun incrementTaps() {
            counter++
        }

        fun event() = when (counter) {
            1 -> MediaEvent.SingleTap
            2 -> MediaEvent.DoubleTap
            else -> MediaEvent.TripleTap
        }
    }
}

internal enum class MediaEvent {
    SingleTap,
    DoubleTap,
    TripleTap,
}
