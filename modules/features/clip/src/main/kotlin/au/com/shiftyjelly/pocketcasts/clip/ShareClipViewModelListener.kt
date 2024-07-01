package au.com.shiftyjelly.pocketcasts.clip

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import kotlin.time.Duration

internal class ShareClipViewModelListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
    private val analyticsTracker: AnalyticsTracker,
) : ShareClipPageListener {
    override fun onClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) {
        viewModel.onClipLinkShared(Clip.fromEpisode(episode, clipRange))
        SharePodcastHelper(
            podcast,
            episode,
            clipRange.start,
            clipRange.end,
            fragment.requireActivity(),
            SharePodcastHelper.ShareType.CLIP,
            SourceView.CLIP_SHARING,
            analyticsTracker,
        ).showShareDialogDirect()
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
