package au.com.shiftyjelly.pocketcasts.sharing.clip

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ShareClipListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
    private val sharingClient: SharingClient,
) : ShareClipPageListener {
    override suspend fun onShareClipLink(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        val request = SharingRequest.clipLink(podcast, episode, clipRange).build()
        val response = sharingClient.share(request)
        if (response.feedbackMessage != null) {
            Toast.makeText(fragment.requireContext(), response.feedbackMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override suspend fun onShareClipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = coroutineScope {
        launch { delay(1.seconds) } // Launch a delay job to allow the loading animation to run even if clipping happens faster
        val request = SharingRequest.audioClip(podcast, episode, clipRange).build()
        val response = sharingClient.share(request)
        if (response.feedbackMessage != null) {
            Toast.makeText(fragment.requireContext(), response.feedbackMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override suspend fun onShareClipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        Toast.makeText(fragment.context, "Share video", Toast.LENGTH_SHORT).show()
    }

    override fun onClickPlay() {
        viewModel.playClip()
    }

    override fun onClickPause() {
        viewModel.pauseClip()
    }

    override fun onUpdateClipStart(duration: Duration) {
        viewModel.updateClipStart(duration)
    }

    override fun onUpdateClipEnd(duration: Duration) {
        viewModel.updateClipEnd(duration)
    }

    override fun onUpdateClipProgress(duration: Duration) {
        viewModel.updateClipProgress(duration)
    }

    override fun onUpdateTimeline(scale: Float, secondsPerTick: Int) {
        viewModel.updateProgressPollingPeriod(scale, secondsPerTick)
    }

    override fun onClose() {
        fragment.dismiss()
    }
}
