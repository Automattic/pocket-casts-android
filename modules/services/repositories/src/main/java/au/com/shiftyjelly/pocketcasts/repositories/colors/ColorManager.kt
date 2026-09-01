package au.com.shiftyjelly.pocketcasts.repositories.colors

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import timber.log.Timber

@Singleton
class ColorManager @Inject constructor(
    private val staticServiceManager: StaticServiceManager,
    private val podcastManager: PodcastManager,
) {

    companion object {
        // the amount of time to leave between color refresh attempts. This is quite low (30 min) because we write out defaults for shows that are missing artwork, so this should always be present
        private const val MIN_TIME_BETWEEN_REFRESH_ATTEMPTS = (30 * 60 * 1000).toLong()
    }

    fun downloadColors(podcastUuid: String): Single<Optional<ArtworkColors>> {
        return staticServiceManager.getColorsSingle(podcastUuid)
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
            val colors = staticServiceManager.getColors(podcast.uuid) ?: return
            podcastManager.updateColorsBlocking(
                podcast.uuid,
                colors.background,
                colors.tintForLightBg,
                colors.tintForDarkBg,
                colors.fabForLightBg,
                colors.fabForDarkBg,
                colors.linkForLightBg,
                colors.linkForDarkBg,
                System.currentTimeMillis(),
            )
            Timber.i("ColorManager successfully updated colors for podcast ${podcast.uuid}")
        } catch (e: Exception) {
            Timber.e(e, "ColorManager could not update colors for podcast ${podcast.uuid}")
        }
    }
}
