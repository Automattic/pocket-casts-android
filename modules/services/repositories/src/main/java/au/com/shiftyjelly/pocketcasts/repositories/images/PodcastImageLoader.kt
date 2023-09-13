package au.com.shiftyjelly.pocketcasts.repositories.images

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import kotlinx.coroutines.runBlocking
import java.io.File
import au.com.shiftyjelly.pocketcasts.images.R as IR

open class PodcastImageLoader(
    private val context: Context,
    private val isDarkTheme: Boolean,
    private val transformations: List<Transformation>,
) {
    private val placeholderSmallThemedDrawableId: Int = if (isDarkTheme) IR.drawable.defaultartwork_small_dark else IR.drawable.defaultartwork_small
    private val placeholderLargeThemedDrawableId: Int = if (isDarkTheme) IR.drawable.defaultartwork_dark else IR.drawable.defaultartwork

    private var useSmallPlaceholder: Boolean = false
    private var onlyDarkTheme: Boolean = false

    var radiusPx: Int = 0
    var shouldScale: Boolean = true

    fun getBitmap(userEpisode: UserEpisode, size: Int): Bitmap? {
        return runBlocking {
            try {
                val request = load(userEpisode).size(size, size).build()
                return@runBlocking context.imageLoader.execute(request).drawable!!.toBitmap()
            } catch (e: Exception) {
                val request = loadNoPodcastCoil().size(size, size).build()
                return@runBlocking context.imageLoader.execute(request).drawable?.toBitmap()
            }
        }
    }

    fun getBitmap(podcast: Podcast, size: Int): Bitmap? {
        return runBlocking {
            try {
                val request = load(podcast).size(size, size).build()
                return@runBlocking context.imageLoader.execute(request).drawable!!.toBitmap()
            } catch (e: Exception) {
                val request = loadNoPodcastCoil().size(size, size).build()
                return@runBlocking context.imageLoader.execute(request).drawable?.toBitmap()
            }
        }
    }

    suspend fun getBitmapSuspend(podcast: Podcast, size: Int): Bitmap? {
        return try {
            val request = load(podcast).size(size, size).build()
            context.imageLoader.execute(request).drawable!!.toBitmap()
        } catch (e: Exception) {
            val request = loadNoPodcastCoil().size(size, size).build()
            context.imageLoader.execute(request).drawable?.toBitmap()
        }
    }

    fun loadForTarget(podcast: Podcast, size: Int, bitmapListener: coil.target.Target) {
        val request = load(podcast).size(size, size).target(bitmapListener).build()
        context.imageLoader.enqueue(request)
    }

    fun loadForTarget(userEpisode: UserEpisode, size: Int, bitmapListener: coil.target.Target) {
        val request = load(userEpisode).size(size, size).target(bitmapListener).build()
        context.imageLoader.enqueue(request)
    }

    fun onlyDarkTheme(): PodcastImageLoader {
        this.onlyDarkTheme = true
        return this
    }

    fun smallPlaceholder(): PodcastImageLoader {
        this.useSmallPlaceholder = true
        return this
    }

    fun largePlaceholder(): PodcastImageLoader {
        this.useSmallPlaceholder = false
        return this
    }

    fun loadCoil(podcastUuid: String?, size: Int? = null, placeholder: Boolean = true, onComplete: () -> Unit = {}): ImageRequest.Builder {
        if (podcastUuid == null) return loadNoPodcastCoil()
        val url = if (size != null) {
            PodcastImage.getArtworkUrl(size = size, uuid = podcastUuid)
        } else {
            PodcastImage.getLargeArtworkUrl(uuid = podcastUuid, context = context)
        }

        val placeholderDrawable = if (placeholder) placeholderResId() else 0
        var builder = ImageRequest.Builder(context)
            .data(url)
            .placeholder(placeholderDrawable)
            .error(placeholderDrawable)
            .transformations()
            .listener(onSuccess = { _, _ -> onComplete() })

        if (!shouldScale) {
            builder = builder.size(Size.ORIGINAL)
        }

        val imageTransformation = buildList {
            add(RoundedCornersTransformation(radiusPx.toFloat()))
            addAll(transformations)
        }

        builder = builder.transformations(imageTransformation)

        return builder
    }

    @JvmOverloads
    fun load(podcast: Podcast?, onComplete: () -> Unit = {}): ImageRequest.Builder {
        return loadCoil(podcast?.uuid, onComplete = onComplete)
    }

    fun loadPodcastUuid(podcastUuid: String?): ImageRequest.Builder {
        return loadCoil(podcastUuid)
    }

    fun load(episode: PodcastEpisode): ImageRequest.Builder {
        return loadCoil(episode.podcastUuid)
    }

    fun load(userEpisode: UserEpisode, thumbnail: Boolean = false): ImageRequest.Builder {
        val builder: ImageRequest.Builder
        val artworkUrl = userEpisode.getUrlForArtwork(isDarkTheme, thumbnail)
        builder = if (artworkUrl.startsWith("/")) {
            ImageRequest.Builder(context).data(File(artworkUrl))
        } else {
            ImageRequest.Builder(context).data(artworkUrl)
        }

        return builder
    }

    fun load(episode: BaseEpisode): ImageRequest.Builder {
        return when (episode) {
            is PodcastEpisode -> {
                load(episode)
            }
            is UserEpisode -> {
                load(episode)
            }
            else -> {
                loadNoPodcastCoil()
            }
        }
    }

    private fun loadNoPodcastCoil(): ImageRequest.Builder {
        val drawableId = placeholderResId()
        return ImageRequest.Builder(context)
            .data(drawableId)
    }

    fun loadSmallImage(podcastUuid: String?): ImageRequest.Builder {
        return loadCoil(podcastUuid = podcastUuid, size = 128)
    }

    fun loadSmallImage(podcast: Podcast?): ImageRequest.Builder {
        return loadSmallImage(podcast?.uuid)
    }

    fun loadLargeImage(podcast: Podcast, imageView: ImageView) {
        return loadCoil(podcast.uuid).into(imageView)
    }

    private fun placeholderResId() =
        when {
            onlyDarkTheme -> IR.drawable.defaultartwork_small_dark
            useSmallPlaceholder -> placeholderSmallThemedDrawableId
            else -> placeholderLargeThemedDrawableId
        }

    fun cacheSubscribedArtwork(podcast: Podcast) {
        val request = cacheSubscribedArtworkRequest(podcast)
        context.imageLoader.enqueue(request)
    }

    suspend fun cacheSubscribedArtworkSuspend(podcast: Podcast) {
        val request = cacheSubscribedArtworkRequest(podcast)
        context.imageLoader.execute(request)
    }

    private fun cacheSubscribedArtworkRequest(podcast: Podcast): ImageRequest {
        return ImageRequest.Builder(context)
            .data(PodcastImage.getLargeArtworkUrl(uuid = podcast.uuid, context = context))
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
    }
}

fun ImageRequest.Builder.into(imageView: ImageView) {
    val request = this.target(imageView).build()
    request.context.imageLoader.enqueue(request)
}
