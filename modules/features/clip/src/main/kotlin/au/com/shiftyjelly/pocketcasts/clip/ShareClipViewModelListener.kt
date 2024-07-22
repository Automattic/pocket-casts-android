package au.com.shiftyjelly.pocketcasts.clip

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlin.time.Duration

internal class ShareClipViewModelListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
) : ShareClipPageListener {
    override fun onClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        viewModel.onClipLinkShared(Clip.fromEpisode(episode, clipRange))
        Toast.makeText(fragment.requireActivity(), "Share clip link", Toast.LENGTH_SHORT).show()
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
