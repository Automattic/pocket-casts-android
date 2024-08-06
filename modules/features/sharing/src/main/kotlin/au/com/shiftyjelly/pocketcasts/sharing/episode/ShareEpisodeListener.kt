package au.com.shiftyjelly.pocketcasts.sharing.episode

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ShareEpisodeListener @AssistedInject constructor(
    @Assisted private val fragment: ShareEpisodeFragment,
    @Assisted private val assetController: BackgroundAssetController,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : ShareEpisodePageListener {
    override suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, platform: SocialPlatform, cardType: CardType): SharingResponse {
        val builder = SharingRequest.episode(podcast, episode)
            .setPlatform(platform)
            .setCardType(cardType)
            .setSourceView(sourceView)
        val request = if (platform == SocialPlatform.Instagram) {
            assetController.capture(cardType).map { builder.setBackgroundImage(it).build() }
        } else {
            Result.success(builder.build())
        }
        return request
            .map { sharingClient.share(it) }
            .getOrElse { error ->
                SharingResponse(
                    isSuccsessful = false,
                    feedbackMessage = fragment.getString(LR.string.error),
                    error = error,
                )
            }
    }

    override fun onClose() {
        fragment.dismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            fragment: ShareEpisodeFragment,
            assetController: BackgroundAssetController,
            sourceView: SourceView,
        ): ShareEpisodeListener
    }
}
