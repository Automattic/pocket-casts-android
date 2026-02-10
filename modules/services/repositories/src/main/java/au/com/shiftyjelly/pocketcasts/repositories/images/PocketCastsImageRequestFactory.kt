package au.com.shiftyjelly.pocketcasts.repositories.images

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.EpisodeFileMetadata
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.transform.Transformation
import java.io.File
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode as PodcastEpisodeEntity
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode as UserEpisodeEntity

data class PocketCastsImageRequestFactory(
    val context: Context,
    private val isDarkTheme: Boolean = false,
    private val cornerRadius: Int = 0,
    private val size: Int? = null,
    private val placeholderType: PlaceholderType = PlaceholderType.Large,
    private val transformations: List<Transformation> = emptyList(),
    private val showErrorPlaceholder: Boolean = true,
) {
    private val actualCornerRadius = cornerRadius.dpToPx(context)
    private val actualSize = size?.dpToPx(context)?.takeIf { it > 0 }

    fun smallSize() = copy(size = 128)

    fun createForPodcast(
        podcastUuid: String?,
        onSuccess: () -> Unit = {},
    ) = create(RequestType.Podcast(podcastUuid), onSuccess)

    fun createForFileOrUrl(
        filePathOrUrl: String,
        onSuccess: () -> Unit = {},
    ) = create(RequestType.FileOrUrl(filePathOrUrl), onSuccess)

    fun create(
        podcast: PodcastEntity,
        onSuccess: () -> Unit = {},
    ) = create(RequestType.Podcast(podcast.uuid), onSuccess)

    fun create(
        episode: BaseEpisode,
        useEpisodeArtwork: Boolean,
        onSuccess: () -> Unit = {},
    ) = when (episode) {
        is PodcastEpisodeEntity -> create(RequestType.PodcastEpisode(episode, useEpisodeArtwork), onSuccess)
        is UserEpisodeEntity -> create(RequestType.UserEpisode(episode, useEpisodeArtwork), onSuccess)
    }

    private fun create(
        type: RequestType,
        onSuccess: () -> Unit,
    ) = ImageRequest.Builder(context)
        .data(type.data(context))
        .let { if (placeholderType == PlaceholderType.None) it else it.placeholder(placeholderId) }
        .let { if (showErrorPlaceholder) it.error(if (isDarkTheme) IR.drawable.defaultartwork_dark else IR.drawable.defaultartwork) else it }
        .transformations(
            buildList {
                add(RoundedCornersTransformation(actualCornerRadius.toFloat()))
                addAll(transformations)
            },
        )
        .let { if (actualSize != null) it.size(actualSize) else it }
        .listener(type.listener(context, onSuccess))
        .build()

    @get:DrawableRes private val placeholderId
        get() = when (placeholderType) {
            PlaceholderType.None -> 0
            PlaceholderType.Small -> if (isDarkTheme) IR.drawable.defaultartwork_small_dark else IR.drawable.defaultartwork_small
            PlaceholderType.Large -> if (isDarkTheme) IR.drawable.defaultartwork_dark else IR.drawable.defaultartwork
        }

    private fun RequestType.data(context: Context) = when (this) {
        is RequestType.Podcast -> data(context)
        is RequestType.PodcastEpisode -> data(context)
        is RequestType.UserEpisode -> data()
        is RequestType.FileOrUrl -> filePathOrUrl
    }

    private fun RequestType.Podcast.data(context: Context) = podcastUuid?.let { podcastArtworkUrl(context, it) } ?: placeholderId

    private fun RequestType.PodcastEpisode.data(context: Context) = if (useEpisodeArtwork) {
        episode.imageUrl ?: EpisodeFileMetadata.artworkCacheFile(context, episode.uuid).takeIf(File::exists) ?: episode.podcastArtworkUrl(context)
    } else {
        episode.podcastArtworkUrl(context)
    }

    private fun RequestType.UserEpisode.data() = if (useEpisodeArtwork) {
        EpisodeFileMetadata.artworkCacheFile(context, episode.uuid).takeIf(File::exists) ?: episode.artworkUrl()
    } else {
        episode.artworkUrl()
    }

    private fun PodcastEpisodeEntity.podcastArtworkUrl(context: Context) = podcastArtworkUrl(context, podcastUuid)

    private fun UserEpisodeEntity.artworkUrl(): String {
        val tintColorIndex = tintColorIndex
        val artworkUrl = artworkUrl
        return if (tintColorIndex == 0 && artworkUrl != null) {
            artworkUrl
        } else {
            val themeType = if (isDarkTheme) "dark" else "light"
            val urlSize = when {
                actualSize == null -> 960
                actualSize > 280 -> 960
                else -> 280
            }
            "${Settings.SERVER_STATIC_URL}/discover/images/artwork/$themeType/$urlSize/$tintColorIndex.png"
        }
    }

    private fun podcastArtworkUrl(context: Context, podcastUuid: String): String {
        return PodcastImage.getArtworkUrl(size = actualSize, uuid = podcastUuid, isWearOS = Util.isWearOs(context))
    }

    private fun RequestType.listener(context: Context, onSuccess: () -> Unit): ImageRequest.Listener? = when (this) {
        is RequestType.PodcastEpisode -> RetryWithPodcastListener(episode.podcastArtworkUrl(context), onSuccess)

        else -> object : ImageRequest.Listener {
            override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess()
        }
    }

    enum class PlaceholderType {
        None,
        Small,
        Large,
    }
}

fun ImageRequest.loadInto(view: ImageView) = context.imageLoader.enqueue(newBuilder().target(view).build())

private sealed interface RequestType {
    data class Podcast(val podcastUuid: String?) : RequestType
    data class PodcastEpisode(val episode: PodcastEpisodeEntity, val useEpisodeArtwork: Boolean) : RequestType
    data class UserEpisode(val episode: UserEpisodeEntity, val useEpisodeArtwork: Boolean) : RequestType
    data class FileOrUrl(val filePathOrUrl: String) : RequestType
}

private class RetryWithPodcastListener(
    private val url: String,
    private val onSuccess: () -> Unit,
) : ImageRequest.Listener {
    override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess()

    override fun onError(request: ImageRequest, result: ErrorResult) {
        val newRequest = request.newBuilder()
            .data(url)
            .listener(null)
            .build()
        request.context.imageLoader.enqueue(newRequest)
    }
}
