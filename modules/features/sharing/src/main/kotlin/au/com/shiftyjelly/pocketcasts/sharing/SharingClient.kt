package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.SERVER_SHORT_URL
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.Instagram
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.More
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.PocketCasts
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.Telegram
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.Tumblr
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.WhatsApp
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform.X
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import coil.executeBlocking
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastModel
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode as EpisodeModel

class SharingClient(
    private val context: Context,
    private val displayPodcastCover: Boolean,
    private val showCustomCopyFeedback: Boolean,
    private val hostUrl: String,
    private val shareStarter: ShareStarter,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
    ) : this(
        context = context,
        displayPodcastCover = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        showCustomCopyFeedback = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2,
        hostUrl = SERVER_SHORT_URL,
        shareStarter = object : ShareStarter {
            override fun start(context: Context, intent: Intent) {
                context.startActivity(intent)
            }

            override fun copyLink(context: Context, data: ClipData) {
                requireNotNull(context.getSystemService<ClipboardManager>()).setPrimaryClip(data)
            }
        },
    )

    private val imageRequestFactory = PocketCastsImageRequestFactory(context, isDarkTheme = false).smallSize()

    suspend fun share(request: SharingRequest) = try {
        Timber.tag("SharingClient").i("Share: $request")
        request.tryShare()
    } catch (t: Throwable) {
        Timber.tag("SharingClient").e(t, "Failed to share a request: $request")
        SharingResponse(
            isSuccsessful = false,
            feedbackMessage = t.message,
        )
    }

    private suspend fun SharingRequest.tryShare(): SharingResponse = when (platform) {
        Instagram -> {
            error("Not implemented yet")
        }

        PocketCasts -> {
            shareStarter.copyLink(context, ClipData.newPlainText(context.getString(data.linkDescription()), data.sharingUrl(hostUrl)))
            SharingResponse(
                isSuccsessful = true,
                feedbackMessage = if (showCustomCopyFeedback) context.getString(LR.string.share_link_copied_feedback) else null,
            )
        }

        WhatsApp, Telegram, X, Tumblr, More -> {
            Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(EXTRA_TEXT, data.sharingUrl(hostUrl))
                .putExtra(EXTRA_TITLE, data.sharingTitle())
                .setPackage(platform.packageId)
                .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                .setPodcastCover(data.podcast)
                .share()
            SharingResponse(
                isSuccsessful = true,
                feedbackMessage = null,
            )
        }
    }

    private fun Intent.share() {
        shareStarter.start(context, toChooserIntent())
    }

    private fun Intent.toChooserIntent() = Intent
        .createChooser(this, context.getString(LR.string.podcasts_share_via))
        .addFlags(FLAG_ACTIVITY_NEW_TASK)

    private suspend fun Intent.setPodcastCover(podcast: Podcast) = apply {
        if (displayPodcastCover) {
            val coverUri = withContext(Dispatchers.IO) {
                runCatching {
                    val request = imageRequestFactory.create(podcast)
                    context.imageLoader.executeBlocking(request).drawable?.toBitmap()?.let { bitmap ->
                        val imageFile = File(context.cacheDir, "share_podcast_thumbnail.jpg")
                        FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                        FileUtil.getUriForFile(context, imageFile)
                    }
                }.getOrNull()
            }
            if (coverUri != null) {
                clipData = ClipData.newRawUri(null, coverUri)
            }
        }
    }
}

data class SharingRequest internal constructor(
    internal val data: SharingRequest.Data,
    internal val platform: SocialPlatform,
    internal val cardType: CardType?,
    internal val source: SourceView,
) {
    companion object {
        fun podcast(podcast: PodcastModel) = Builder(Data.Podcast(podcast))

        fun episode(podcast: PodcastModel, episode: PodcastEpisode) = Builder(Data.Episode(podcast, episode))
    }

    class Builder internal constructor(
        private var data: SharingRequest.Data,
    ) {
        private var platform = More
        private var cardType: CardType? = null
        private var source = SourceView.UNKNOWN

        fun setPlatform(platform: SocialPlatform) = apply {
            this.platform = platform
        }

        fun setCardType(cardType: CardType) = apply {
            this.cardType = cardType
        }

        fun setSourceView(source: SourceView) = apply {
            this.source = source
        }

        fun build() = SharingRequest(
            data = data,
            platform = platform,
            cardType = cardType,
            source = source,
        )
    }

    internal sealed interface Data {
        val podcast: PodcastModel

        fun sharingUrl(host: String): String

        fun sharingTitle(): String

        @StringRes fun linkDescription(): Int

        data class Podcast(
            override val podcast: PodcastModel,
        ) : Data {
            override fun sharingUrl(host: String) = "$host/podcast/${podcast.uuid}"

            override fun sharingTitle() = podcast.title

            override fun linkDescription() = LR.string.share_link_podcast

            override fun toString() = "Podcast(title=${podcast.title},uuid=${podcast.uuid})"
        }

        data class Episode(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
        ) : Data {
            override fun sharingUrl(host: String) = "$host/episode/${episode.uuid}"

            override fun sharingTitle() = episode.title

            override fun linkDescription() = LR.string.share_link_episode

            override fun toString() = "Podcast(title=${episode.title},uuid=${episode.uuid})"
        }
    }
}

data class SharingResponse(
    val isSuccsessful: Boolean,
    val feedbackMessage: String?,
)
