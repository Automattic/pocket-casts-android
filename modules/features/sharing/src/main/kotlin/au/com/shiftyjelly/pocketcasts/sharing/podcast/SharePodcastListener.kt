package au.com.shiftyjelly.pocketcasts.sharing.podcast

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class SharePodcastListener @AssistedInject constructor(
    @Assisted private val fragment: SharePodcastFragment,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : SharePodcastPageListener {
    override suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: CardType): SharingResponse {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(platform)
            .setCardType(cardType)
            .setSourceView(sourceView)
            .build()
        return sharingClient.share(request)
    }

    override fun onClose() {
        fragment.dismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            fragment: SharePodcastFragment,
            sourceView: SourceView,
        ): SharePodcastListener
    }
}
