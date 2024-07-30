package au.com.shiftyjelly.pocketcasts.sharing.clip

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlin.time.Duration

internal class ShareClipListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
) : ShareClipPageListener {
    override suspend fun onShareClipLink(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        Toast.makeText(fragment.context, "Share link", Toast.LENGTH_SHORT).show()
    }

    override suspend fun onShareClipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        Toast.makeText(fragment.context, "Share audio", Toast.LENGTH_SHORT).show()
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
