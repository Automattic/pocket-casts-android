package au.com.shiftyjelly.pocketcasts.sharing.episode

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class ShareEpisodeListener @AssistedInject constructor(
    @Assisted private val fragment: ShareEpisodeFragment,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : ShareEpisodePageListener {
    override fun onShare(podcast: Podcast, episode: PodcastEpisode, platform: SocialPlatform, cardType: CardType) {
        val request = SharingRequest.episode(podcast, episode)
            .setPlatform(platform)
            .setCardType(cardType)
            .setSourceView(sourceView)
            .build()
        fragment.lifecycleScope.launch {
            val response = sharingClient.share(request)
            if (response.feedbackMessage != null) {
                Toast.makeText(fragment.requireActivity(), response.feedbackMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClose() {
        fragment.dismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            fragment: ShareEpisodeFragment,
            sourceView: SourceView,
        ): ShareEpisodeListener
    }
}
