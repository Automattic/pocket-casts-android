package au.com.shiftyjelly.pocketcasts.sharing.podcast

import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform

internal class SharePodcastListener(
    private val fragment: SharePodcastFragment,
) : SharePodcastPageListener {
    override fun onShare(podcast: Podcast, platform: SocialPlatform) {
        Toast.makeText(fragment.requireActivity(), "Share ${podcast.title} to $platform", Toast.LENGTH_SHORT).show()
    }

    override fun onClose() {
        fragment.dismiss()
    }
}
