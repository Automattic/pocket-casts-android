package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.ShareActions
import kotlin.time.Duration
import kotlinx.coroutines.launch

internal class ShareClipViewModelListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
    private val shareActions: ShareActions,
) : ShareClipPageListener {
    override fun onClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        viewModel.onClipLinkShared(Clip.fromEpisode(episode, clipRange))
        fragment.lifecycleScope.launch {
            shareActions.shareClipLink(podcast, episode, clipRange.start, clipRange.end)
        }
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
