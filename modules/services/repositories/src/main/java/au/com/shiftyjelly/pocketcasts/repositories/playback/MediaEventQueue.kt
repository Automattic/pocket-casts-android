package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class MediaEventQueue(
    private val scope: CoroutineScope,
) {
    private var singleTapJob: SingleTapJob? = null
    private var multiTapJob: Job? = null

    suspend fun consumeEvent(event: MediaEvent) = when (event) {
        MediaEvent.SingleTap -> handleSingleTapEvent()
        MediaEvent.DoubleTap, MediaEvent.TripleTap -> handleMultiTapEvent(event)
    }

    private suspend fun handleSingleTapEvent(): MediaEvent? {
        val currentSingleTapJob = singleTapJob
        return when {
            // Pixel Buds (and possibly other headphones) trigger KEYCODE_MEDIA_PLAY
            // after KEYCODE_MEDIA_NEXT or KEYCODE_MEDIA_PREVIOUS.
            // We need to ignore it so the single tap action isn't triggered in such cases.
            multiTapJob?.isActive == true -> {
                null
            }

            currentSingleTapJob?.isActive == true -> {
                currentSingleTapJob.incrementTaps()
                null
            }

            else -> {
                val newSingleTapJob = SingleTapJob(scope)
                singleTapJob = newSingleTapJob
                newSingleTapJob.await()
                newSingleTapJob.event()
            }
        }
    }

    private fun handleMultiTapEvent(event: MediaEvent): MediaEvent {
        val currentJob = multiTapJob
        multiTapJob = scope.launch { delay(250) }
        currentJob?.cancel()
        return event
    }

    private class SingleTapJob(
        private val scope: CoroutineScope,
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
