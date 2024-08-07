package au.com.shiftyjelly.pocketcasts.sharing.podcast

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class SharePodcastListener @AssistedInject constructor(
    @Assisted private val fragment: SharePodcastFragment,
    @Assisted private val assetController: BackgroundAssetController,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : SharePodcastPageListener {
    override suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: VisualCardType): SharingResponse {
        val builder = SharingRequest.podcast(podcast)
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
            fragment: SharePodcastFragment,
            assetController: BackgroundAssetController,
            sourceView: SourceView,
        ): SharePodcastListener
    }
}
