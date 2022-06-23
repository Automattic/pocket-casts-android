package au.com.shiftyjelly.pocketcasts.repositories.colors

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ColorManager @Inject constructor(
    private val staticServerManager: StaticServerManagerImpl,
    private val podcastManager: PodcastManager
) {

    companion object {
        const val DEFAULT_BACKGROUND_COLOR = 0xFF3D3D3D.toInt()

        // the amount of time to leave between color refresh attempts. This is quite low (30 min) because we write out defaults for shows that are missing artwork, so this should always be present
        private const val MIN_TIME_BETWEEN_REFRESH_ATTEMPTS = (30 * 60 * 1000).toLong()

        fun getBackgroundColor(podcast: Podcast?): Int = colorOrDefault(podcast?.backgroundColor, DEFAULT_BACKGROUND_COLOR)

        private fun colorOrDefault(color: Int?, defaultColor: Int): Int {
            return if (color == null || color == 0) {
                defaultColor
            } else {
                color
            }
        }
    }

    fun downloadColors(podcastUuid: String): Single<Optional<ArtworkColors>> {
        return staticServerManager.getColorsSingle(podcastUuid)
    }

    suspend fun updateColors(podcasts: List<Podcast>) {
        podcasts.forEach { updatePodcastColors(it) }
    }

    private suspend fun updatePodcastColors(podcast: Podcast?) {
        podcast ?: return

        if (abs(System.currentTimeMillis() - podcast.colorLastDownloaded) < MIN_TIME_BETWEEN_REFRESH_ATTEMPTS) {
            return
        }

        try {
            val colors = staticServerManager.getColors(podcast.uuid) ?: return
            podcastManager.updateColors(
                podcast.uuid,
                colors.background,
                colors.tintForLightBg,
                colors.tintForDarkBg,
                colors.fabForLightBg,
                colors.fabForDarkBg,
                colors.linkForLightBg,
                colors.linkForDarkBg,
                System.currentTimeMillis()
            )
            Timber.i("ColorManager successfully updated colors for podcast ${podcast.uuid}")
        } catch (e: Exception) {
            Timber.e(e, "ColorManager could not update colors for podcast ${podcast.uuid}")
        }
    }
}
