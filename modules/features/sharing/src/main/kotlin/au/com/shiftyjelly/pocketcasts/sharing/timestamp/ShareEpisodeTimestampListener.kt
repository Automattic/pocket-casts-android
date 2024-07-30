package au.com.shiftyjelly.pocketcasts.sharing.timestamp

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.time.Duration

internal class ShareEpisodeTimestampListener @AssistedInject constructor(
    @Assisted private val fragment: ShareEpisodeTimestampFragment,
    @Assisted private val type: TimestampType,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : ShareEpisodeTimestampPageListener {
    override suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform, cardType: CardType): SharingResponse {
        val builder = when (type) {
            TimestampType.Episode -> SharingRequest.episodePosition(podcast, episode, timestamp)
            TimestampType.Bookmark -> SharingRequest.bookmark(podcast, episode, timestamp)
        }
        val request = builder
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
            fragment: ShareEpisodeTimestampFragment,
            type: TimestampType,
            sourceView: SourceView,
        ): ShareEpisodeTimestampListener
    }
}
