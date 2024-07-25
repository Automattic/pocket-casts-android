package au.com.shiftyjelly.pocketcasts.sharing.timestamp

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import kotlin.time.Duration

internal class ShareEpisodeTimestampListener(
    private val type: TimestampType,
    private val fragment: ShareEpisodeTimestampFragment,
) : ShareEpisodeTimestampPageListener {
    override fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform) {
        Toast.makeText(fragment.requireActivity(), "Share $timestamp as $type to $platform", Toast.LENGTH_SHORT).show()
    }

    override fun onClose() {
        fragment.dismiss()
    }
}
