package au.com.shiftyjelly.pocketcasts.repositories.images

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.images.RoundedCornersTransformation
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.target.Target
import coil.transform.Transformation
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode as PodcastEpisodeEntity
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode as UserEpisodeEntity

data class PocketCastsImageRequestFactory(
    val context: Context,
    private val isDarkTheme: Boolean = false,
    @Px private val cornerRadius: Int = 0,
    @Px private val size: Int? = null,
    private val placeholderType: PlaceholderType = PlaceholderType.Large,
    private val transformations: List<Transformation> = emptyList(),
) {
    private val actualCornerRadius = cornerRadius.dpToPx(context)
    private val actualSize = size?.dpToPx(context)

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
        useRssArtwork: Boolean,
        onSuccess: () -> Unit = {},
    ) = when (episode) {
        is PodcastEpisodeEntity -> create(RequestType.PodcastEpisode(episode, useRssArtwork), onSuccess)
        is UserEpisodeEntity -> create(RequestType.UserEpisode(episode), onSuccess)
    }

    private fun create(
        type: RequestType,
        onSuccess: () -> Unit,
    ) = ImageRequest.Builder(context)
        .data(type.data(context))
        .placeholder(placeholderId)
        .error(placeholderId)
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
        is RequestType.FileOrUrl -> filePathOrUrl.toHttpUrlOrNull() ?: File(filePathOrUrl)
    }

    private fun RequestType.Podcast.data(context: Context) = podcastUuid?.let { podcastArtworkUrl(context, it) } ?: placeholderId

    private fun RequestType.PodcastEpisode.data(context: Context) = if (useRssArtwork) {
        episode.imageUrl ?: episode.podcastArtworkUrl(context)
    } else {
        episode.podcastArtworkUrl(context)
    }

    private fun RequestType.UserEpisode.data(): String {
        val tintColorIndex = episode.tintColorIndex
        val artworkUrl = episode.artworkUrl
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

    private fun PodcastEpisodeEntity.podcastArtworkUrl(context: Context) = podcastArtworkUrl(context, podcastUuid)

    private fun podcastArtworkUrl(context: Context, podcastUuid: String): String {
        val maxSize = if (Util.isWearOs(context)) 480 else 960
        val urlSize = when {
            actualSize == null -> maxSize
            actualSize > 480 -> maxSize
            actualSize > 200 -> 480
            else -> 200
        }
        return "${Settings.SERVER_STATIC_URL}/discover/images/webp/$urlSize/$podcastUuid.webp"
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

fun ImageRequest.loadInto(target: Target) = context.imageLoader.enqueue(newBuilder().target(target).build())

private sealed interface RequestType {
    data class Podcast(val podcastUuid: String?) : RequestType
    data class PodcastEpisode(val episode: PodcastEpisodeEntity, val useRssArtwork: Boolean) : RequestType
    data class UserEpisode(val episode: UserEpisodeEntity) : RequestType
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
