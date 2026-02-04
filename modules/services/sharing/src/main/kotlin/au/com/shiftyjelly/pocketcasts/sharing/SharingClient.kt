package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ReferralsDeepLink
import au.com.shiftyjelly.pocketcasts.localization.helper.StatsHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.META_APP_ID
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.SERVER_SHORT_URL
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.WEB_BASE_HOST
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.Instagram
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.More
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.PocketCasts
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.Telegram
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.Tumblr
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.WhatsApp
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform.X
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.toSecondsWithSingleMilli
import coil3.executeBlocking
import coil3.imageLoader
import coil3.toBitmap
import com.automattic.eventhorizon.EndOfYearShareSource
import com.automattic.eventhorizon.EndOfYearStoryShareEvent
import com.automattic.eventhorizon.EndOfYearStorySharedEvent
import com.automattic.eventhorizon.PodcastSharedEvent
import com.automattic.eventhorizon.ReferralPassSharedEvent
import com.automattic.eventhorizon.ShareActionCardType
import com.automattic.eventhorizon.ShareActionPlatform
import com.automattic.eventhorizon.ShareActionType
import com.automattic.eventhorizon.Trackable
import com.automattic.eventhorizon.TranscriptSharedEvent
import java.io.File
import java.io.FileOutputStream
import java.time.Year
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastModel
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode as EpisodeModel

class SharingClient(
    private val context: Context,
    private val mediaService: MediaService,
    private val listeners: Set<Listener>,
    private val displayPodcastCover: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    private val showCustomCopyFeedback: Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2,
    private val hostUrl: String = SERVER_SHORT_URL,
    private val webBasedHost: String = WEB_BASE_HOST,
    private val metaAppId: String = META_APP_ID,
    private val shareStarter: ShareStarter = object : ShareStarter {
        override fun start(context: Context, intent: Intent) {
            context.startActivity(intent)
        }

        override fun copyLink(context: Context, data: ClipData) {
            requireNotNull(context.getSystemService<ClipboardManager>()).setPrimaryClip(data)
        }
    },
) {
    private val imageRequestFactory = PocketCastsImageRequestFactory(context, isDarkTheme = false).smallSize()

    suspend fun share(request: SharingRequest): SharingResponse {
        listeners.forEach { it.onShare(request) }
        val response = try {
            request.tryShare()
        } catch (error: Throwable) {
            SharingResponse(
                isSuccessful = false,
                feedbackMessage = context.getString(LR.string.share_error_message),
                error = error,
            )
        }
        listeners.forEach { it.onShared(request, response) }
        return response
    }

    private suspend fun SharingRequest.tryShare(): SharingResponse = when (data) {
        is SharingRequest.Sociable -> when (platform) {
            Instagram -> {
                val backgroundImage = requireNotNull(backgroundImage) { "Sharing to Instagram requires a background image" }
                Intent()
                    .setAction("com.instagram.share.ADD_TO_STORY")
                    .putExtra("source_application", metaAppId)
                    .setDataAndType(FileUtil.getUriForFile(context, backgroundImage), "image/png")
                    .addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK)
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            }

            PocketCasts -> {
                shareStarter.copyLink(context, ClipData.newPlainText(context.getString(data.linkDescription()), data.sharingUrl(hostUrl)))
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = if (showCustomCopyFeedback) context.getString(LR.string.share_link_copied_feedback) else null,
                    error = null,
                )
            }

            WhatsApp, Telegram, X, Tumblr, More -> {
                val intent = Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(EXTRA_TEXT, data.sharingUrl(hostUrl))
                    .putExtra(EXTRA_TITLE, data.sharingTitle())
                    .setPackage(platform.packageId)
                    .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                data.podcast?.let {
                    intent.setPodcastCover(it)
                }
                intent
                    .toChooserIntent()
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            }
        }

        is SharingRequest.Data.ReferralLink -> {
            val shareText = "${context.getString(LR.string.referrals_share_text, data.offerName)}\n\n${data.sharingUrl(webBasedHost)}"
            val shareSubject = context.getString(LR.string.referrals_share_subject, data.offerDuration)
            Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(EXTRA_TEXT, shareText)
                .putExtra(EXTRA_SUBJECT, shareSubject)
                .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                .toChooserIntent()
                .share()
            SharingResponse(
                isSuccessful = true,
                feedbackMessage = null,
                error = null,
            )
        }

        is SharingRequest.Data.EpisodeFile -> {
            val file = data.episode.downloadedFilePath?.let(::File)
            if (file?.exists() == true) {
                Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType(data.episode.fileType)
                    .setExtraStream(file)
                    .toChooserIntent()
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            } else {
                SharingResponse(
                    isSuccessful = false,
                    feedbackMessage = context.getString(LR.string.share_error_message),
                    error = null,
                )
            }
        }

        is SharingRequest.Data.ClipLink -> when (platform) {
            PocketCasts -> {
                shareStarter.copyLink(context, ClipData.newPlainText(context.getString(data.linkDescription()), data.sharingUrl(hostUrl)))
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = if (showCustomCopyFeedback) context.getString(LR.string.share_link_copied_feedback) else null,
                    error = null,
                )
            }

            Instagram, WhatsApp, Telegram, X, Tumblr, More -> {
                Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(EXTRA_TEXT, data.sharingUrl(hostUrl))
                    .putExtra(EXTRA_TITLE, data.sharingTitle())
                    .setPackage(platform.packageId)
                    .toChooserIntent()
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            }
        }

        is SharingRequest.Data.ClipAudio -> {
            val file = mediaService.clipAudio(data.podcast, data.episode, data.range).getOrThrow()
            Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("audio/mp3")
                .setExtraStream(file)
                .setPackage(platform.packageId)
                .toChooserIntent()
                .share()
            SharingResponse(
                isSuccessful = true,
                feedbackMessage = null,
                error = null,
            )
        }

        is SharingRequest.Data.ClipVideo -> when (platform) {
            Instagram -> {
                val backgroundImage = requireNotNull(backgroundImage) { "Sharing a video requires a background image" }
                val cardType = requireNotNull(cardType as VisualCardType) { "Video must be shared with a visual card" }
                val file = mediaService.clipVideo(data.podcast, data.episode, data.range, cardType, backgroundImage).getOrThrow()
                Intent()
                    .setAction("com.instagram.share.ADD_TO_STORY")
                    .putExtra("source_application", metaAppId)
                    .setDataAndType(FileUtil.getUriForFile(context, file), "video/mp4")
                    .addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK)
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            }

            WhatsApp, Telegram, X, Tumblr, PocketCasts, More -> {
                val backgroundImage = requireNotNull(backgroundImage) { "Sharing a video requires a background image" }
                val cardType = requireNotNull(cardType as VisualCardType) { "Video must be shared with a visual card" }
                val file = mediaService.clipVideo(data.podcast, data.episode, data.range, cardType, backgroundImage).getOrThrow()
                Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType("video/mp4")
                    .setExtraStream(file)
                    .setPackage(platform.packageId)
                    .toChooserIntent()
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            }
        }

        is SharingRequest.Data.EndOfYearStory -> {
            if (data.story.isShareable) {
                val text = buildString {
                    append(data.sharingMessage(context, hostUrl))
                    append(" #pocketcasts #playback")
                    append(data.year.value)
                }
                val pendingIntent = ItemSharedReceiver.intent(
                    context = context,
                    event = EndOfYearStorySharedEvent(
                        from = EndOfYearShareSource.Button,
                        story = data.story.eventHorizonValue,
                        currentYear = data.year.value.toLong(),
                    ),
                )
                Intent()
                    .setAction(Intent.ACTION_SEND)
                    .setType("image/png")
                    .setExtraStream(data.screenshot)
                    .putExtra(EXTRA_TEXT, text)
                    .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                    .toChooserIntent(pendingIntent.intentSender)
                    .share()
                SharingResponse(
                    isSuccessful = true,
                    feedbackMessage = null,
                    error = null,
                )
            } else {
                SharingResponse(
                    isSuccessful = false,
                    feedbackMessage = context.getString(LR.string.end_of_year_cant_share_message),
                    error = null,
                )
            }
        }

        is SharingRequest.Data.Transcript -> {
            val fileName = FileUtil.createSafeFileName(text = data.episodeTitle, fallback = "Transcript")

            val file = FileUtil.writeTextToTempFile(
                fileName = "$fileName.txt",
                text = data.transcript,
                context = context,
            ) ?: return SharingResponse(
                isSuccessful = false,
                feedbackMessage = context.getString(LR.string.share_error_message),
                error = null,
            )

            Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .setExtraStream(file)
                .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                .toChooserIntent()
                .share()
            SharingResponse(
                isSuccessful = true,
                feedbackMessage = null,
                error = null,
            )
        }
    }

    private fun Intent.share() {
        shareStarter.start(context, this)
    }

    private fun Intent.toChooserIntent(sender: IntentSender? = null) = Intent
        .createChooser(this, context.getString(LR.string.podcasts_share_via), sender)
        .addFlags(FLAG_ACTIVITY_NEW_TASK)

    private suspend fun Intent.setPodcastCover(podcast: Podcast) = apply {
        if (displayPodcastCover) {
            val coverUri = withContext(Dispatchers.IO) {
                runCatching {
                    val request = imageRequestFactory.create(podcast)
                    context.imageLoader.executeBlocking(request).image?.toBitmap()?.let { bitmap ->
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

    private fun Intent.setExtraStream(file: File) = putExtra(EXTRA_STREAM, FileUtil.createUriWithReadPermissions(context, file, this))

    interface Listener {
        fun onShare(request: SharingRequest) = Unit
        fun onShared(request: SharingRequest, response: SharingResponse) = Unit
    }
}

@ConsistentCopyVisibility
data class SharingRequest internal constructor(
    val data: Data,
    val trackable: Trackable?,
    val platform: SocialPlatform,
    val cardType: CardType?,
    val backgroundImage: File?,
) {
    companion object {
        fun podcast(
            podcast: PodcastModel,
            source: SourceView,
            platform: SocialPlatform,
            cardType: CardType,
        ) = Builder(
            data = Data.Podcast(podcast),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.Podcast,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        )

        fun episode(
            podcast: PodcastModel,
            episode: PodcastEpisode,
            source: SourceView,
            platform: SocialPlatform,
            cardType: CardType,
        ) = Builder(
            data = Data.Episode(podcast, episode),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.Episode,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        )

        fun episodePosition(
            podcast: PodcastModel,
            episode: PodcastEpisode,
            position: Duration,
            source: SourceView,
            platform: SocialPlatform,
            cardType: CardType,
        ) = Builder(
            data = Data.EpisodePosition(podcast, episode, position, TimestampType.Episode),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.CurrentTime,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        )

        fun bookmark(
            podcast: PodcastModel,
            episode: PodcastEpisode,
            position: Duration,
            source: SourceView,
            platform: SocialPlatform,
            cardType: CardType,
        ) = Builder(
            data = Data.EpisodePosition(podcast, episode, position, TimestampType.Bookmark),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.BookmarkTime,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        )

        fun episodeFile(
            podcast: Podcast,
            episode: PodcastEpisode,
            source: SourceView,
        ) = Builder(
            data = Data.EpisodeFile(podcast, episode),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.EpisodeFile,
                action = ShareActionPlatform.SystemSheet,
            ),
        )

        fun clipLink(
            podcast: Podcast,
            episode: PodcastEpisode,
            range: Clip.Range,
            source: SourceView,
            platform: SocialPlatform,
            cardType: CardType,
        ) = Builder(
            data = Data.ClipLink(podcast, episode, range),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.ClipLink,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        )

        fun audioClip(
            podcast: Podcast,
            episode: PodcastEpisode,
            range: Clip.Range,
            source: SourceView,
        ) = Builder(
            data = Data.ClipAudio(podcast, episode, range),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.ClipAudio,
                action = ShareActionPlatform.SystemSheet,
                cardType = ShareActionCardType.Audio,
            ),
            platform = More,
            cardType = CardType.Audio,
        )

        fun videoClip(
            podcast: Podcast,
            episode: PodcastEpisode,
            range: Clip.Range,
            backgroundImage: File,
            source: SourceView,
            platform: SocialPlatform,
            cardType: VisualCardType,
        ) = Builder(
            data = Data.ClipVideo(podcast, episode, range),
            trackable = PodcastSharedEvent(
                source = source.eventHorizonValue,
                type = ShareActionType.ClipVideo,
                action = platform.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
            ),
            platform = platform,
            cardType = cardType,
        ).setBackgroundImage(backgroundImage)

        fun referralLink(
            referralCode: String,
            offerName: String,
            offerDuration: String,
            source: SourceView,
        ) = Builder(
            data = Data.ReferralLink(referralCode, offerName, offerDuration),
            trackable = ReferralPassSharedEvent(
                source = source.eventHorizonValue,
                code = referralCode,
            ),
        )

        fun endOfYearStory(
            story: Story,
            year: Year,
            screenshot: File,
        ) = Builder(
            data = Data.EndOfYearStory(story, year, screenshot),
            trackable = EndOfYearStoryShareEvent(
                story = story.eventHorizonValue,
                currentYear = year.value.toLong(),
            ),
        )

        fun transcript(
            podcastUuid: String,
            episodeUuid: String,
            episodeTitle: String,
            transcript: String,
            source: SourceView,
        ) = Builder(
            data = Data.Transcript(episodeUuid, episodeTitle, transcript),
            trackable = TranscriptSharedEvent(
                source = source.eventHorizonValue,
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
            ),
        )
    }

    class Builder internal constructor(
        private val data: Data,
        private val trackable: Trackable,
        private val platform: SocialPlatform = More,
        private val cardType: CardType? = null,
    ) {
        private var backgroundImage: File? = null

        fun setBackgroundImage(backgroundImage: File) = apply {
            this.backgroundImage = backgroundImage
        }

        fun build() = SharingRequest(
            data = data,
            trackable = trackable,
            platform = platform,
            cardType = cardType,
            backgroundImage = backgroundImage,
        )
    }

    internal sealed interface Sociable {
        fun sharingUrl(host: String): String

        fun sharingTitle(): String

        @StringRes fun linkDescription(): Int
    }

    sealed interface Data {
        val podcast: PodcastModel?

        class Podcast internal constructor(
            override val podcast: PodcastModel,
        ) : Data,
            Sociable {
            override fun sharingUrl(host: String) = "$host/podcast/${podcast.uuid}"

            override fun sharingTitle() = podcast.title

            override fun linkDescription() = LR.string.share_link_podcast

            override fun toString() = "Podcast(title=${podcast.title}, uuid=${podcast.uuid})"
        }

        class Episode internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
        ) : Data,
            Sociable {
            override fun sharingUrl(host: String) = "$host/episode/${episode.uuid}"

            override fun sharingTitle() = episode.title

            override fun linkDescription() = LR.string.share_link_episode

            override fun toString() = "Episode(title=${episode.title}, uuid=${episode.uuid})"
        }

        class EpisodePosition internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
            val position: Duration,
            val type: TimestampType,
        ) : Data,
            Sociable {
            override fun sharingUrl(host: String) = "$host/episode/${episode.uuid}?t=${position.inWholeSeconds}"

            override fun sharingTitle() = episode.title

            override fun linkDescription() = when (type) {
                TimestampType.Episode -> LR.string.share_link_episode_position
                TimestampType.Bookmark -> LR.string.share_link_bookmark
            }

            override fun toString() = "EpisodePosition(title=${episode.title}, uuid=${episode.uuid}, position=${position.inWholeSeconds}, type=$type)"
        }

        class EpisodeFile internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
        ) : Data {
            override fun toString() = "EpisodeFile(title=${episode.title}, uuid=${episode.uuid}"
        }

        class ClipLink internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
            val range: Clip.Range,
        ) : Data {
            fun sharingUrl(host: String) = "$host/episode/${episode.uuid}?t=${range.start.toSecondsWithSingleMilli()},${range.end.toSecondsWithSingleMilli()}"

            fun sharingTitle() = episode.title

            fun linkDescription() = LR.string.share_link_clip

            override fun toString() = "ClipLink(title=${episode.title}, uuid=${episode.uuid}, start=${range.start.toSecondsWithSingleMilli()}, end=${range.end.toSecondsWithSingleMilli()})"
        }

        class ClipAudio internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
            val range: Clip.Range,
        ) : Data {
            override fun toString() = "ClipAudio(title=${episode.title}, uuid=${episode.uuid}, start=${range.start.toSecondsWithSingleMilli()}, end=${range.end.toSecondsWithSingleMilli()})"
        }

        class ClipVideo internal constructor(
            override val podcast: PodcastModel,
            val episode: EpisodeModel,
            val range: Clip.Range,
        ) : Data {
            override fun toString() = "ClipVideo(title=${episode.title}, uuid=${episode.uuid}, start=${range.start.toSecondsWithSingleMilli()}, end=${range.end.toSecondsWithSingleMilli()})"
        }

        class ReferralLink internal constructor(
            val referralCode: String,
            val offerName: String,
            val offerDuration: String,
        ) : Data {
            override val podcast = null

            fun sharingUrl(host: String) = ReferralsDeepLink(code = referralCode).toUri(host)

            override fun toString() = "ReferralLink(referralCode=$referralCode"
        }

        class EndOfYearStory internal constructor(
            val story: Story,
            val year: Year,
            val screenshot: File,
        ) : Data {
            override val podcast = null

            fun sharingMessage(
                context: Context,
                shortUrl: String,
            ) = when (story) {
                is Story.PlaceholderWhileLoading -> shortUrl

                is Story.Cover -> shortUrl

                is Story.NumberOfShows -> buildString {
                    append(
                        context.getString(
                            LR.string.end_of_year_story_listened_to_numbers_share_text,
                            story.showCount,
                            story.episodeCount,
                            year.value,
                        ),
                    )
                    append(' ')
                    append(shortUrl)
                }

                is Story.TopShow -> context.getString(
                    LR.string.end_of_year_story_top_podcast_share_text,
                    year.value,
                    "$shortUrl/podcast/${story.show.uuid}",
                )

                is Story.TopShows -> context.getString(LR.string.end_of_year_story_top_podcasts_share_text, story.podcastListUrl ?: shortUrl)

                is Story.Ratings -> buildString {
                    append(
                        context.getString(
                            LR.string.end_of_year_story_ratings_share_text,
                            story.stats.count(),
                            year.value,
                            story.stats.max().first.numericalValue,
                        ),
                    )
                    append(' ')
                    append(shortUrl)
                }

                is Story.TotalTime -> buildString {
                    append(
                        context.getString(
                            LR.string.end_of_year_story_listened_to_share_text,
                            StatsHelper.secondsToFriendlyString(story.duration.inWholeSeconds, context.resources),
                        ),
                    )
                    append(' ')
                    append(shortUrl)
                }

                is Story.LongestEpisode -> context.getString(
                    LR.string.end_of_year_story_longest_episode_share_text,
                    year.value,
                    "$shortUrl/episode/${story.episode.episodeId}",
                )

                is Story.PlusInterstitial -> shortUrl

                is Story.YearVsYear -> buildString {
                    append(
                        context.getString(
                            LR.string.end_of_year_stories_year_over_share_text,
                            year.value,
                            year.value - 1,
                        ),
                    )
                    append(' ')
                    append(shortUrl)
                }

                is Story.CompletionRate -> buildString {
                    append(
                        context.getString(
                            LR.string.end_of_year_stories_completion_rate_share_text,
                            story.completionRatePercentage,
                        ),
                    )
                    append(' ')
                    append(shortUrl)
                }

                is Story.Ending -> shortUrl
            }

            override fun toString() = "EndOfYearStory(story=$story, year=$year)"
        }

        class Transcript internal constructor(
            val episodeUuid: String,
            val episodeTitle: String,
            val transcript: String,
        ) : Data {
            override val podcast: PodcastModel? = null

            override fun toString() = "Transcript(episodeUuid=$episodeUuid)"
        }
    }
}

data class SharingResponse(
    val isSuccessful: Boolean,
    val feedbackMessage: String?,
    val error: Throwable?,
)
