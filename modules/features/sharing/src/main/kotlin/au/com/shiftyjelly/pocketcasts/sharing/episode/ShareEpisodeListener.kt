package au.com.shiftyjelly.pocketcasts.sharing.episode

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform

internal class ShareEpisodeListener(
    private val fragment: ShareEpisodeFragment,
) : ShareEpisodePageListener {
    override fun onShare(podcast: Podcast, episode: PodcastEpisode, platform: SocialPlatform) {
        Toast.makeText(fragment.requireActivity(), "Share ${episode.title} to $platform", Toast.LENGTH_SHORT).show()
    }

    override fun onClose() {
        fragment.dismiss()
    }
}
