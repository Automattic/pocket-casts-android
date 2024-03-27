package au.com.shiftyjelly.pocketcasts.widget.data

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import java.util.Date

internal sealed interface PlayerWidgetEpisode {
    val uuid: String
    val title: String
    val artworkUrl: String?
    val duration: Double
    val playedUpTo: Double
    val durationMs: Int get() = (duration * 1000.0).toInt()
    val playedUpToMs: Int get() = (playedUpTo * 1000.0).toInt()

    fun toBaseEpisode(): BaseEpisode

    @JsonClass(generateAdapter = true)
    data class Podcast(
        override val uuid: String,
        override val title: String,
        override val duration: Double,
        override val playedUpTo: Double,
        override val artworkUrl: String?,
        val podcastUuid: String,
    ) : PlayerWidgetEpisode {
        override fun toBaseEpisode() = PodcastEpisode(
            uuid = uuid,
            title = title,
            duration = duration,
            playedUpTo = playedUpTo,
            podcastUuid = podcastUuid,
            imageUrl = artworkUrl,
            publishedDate = Epoch,
        )
    }

    @JsonClass(generateAdapter = true)
    data class User(
        override val uuid: String,
        override val title: String,
        override val duration: Double,
        override val playedUpTo: Double,
        override val artworkUrl: String?,
        val tintColorIndex: Int,
    ) : PlayerWidgetEpisode {
        override fun toBaseEpisode() = UserEpisode(
            uuid = uuid,
            title = title,
            duration = duration,
            playedUpTo = playedUpTo,
            artworkUrl = artworkUrl,
            tintColorIndex = tintColorIndex,
            publishedDate = Epoch,
        )
    }

    companion object {
        private val Epoch = Date(0)

        val AdapterFactory: PolymorphicJsonAdapterFactory<PlayerWidgetEpisode> = PolymorphicJsonAdapterFactory
            .of(PlayerWidgetEpisode::class.java, "type")
            .withSubtype(Podcast::class.java, "podcast")
            .withSubtype(User::class.java, "user")

        fun fromBaseEpisode(episode: BaseEpisode) = when (episode) {
            is PodcastEpisode -> Podcast(
                uuid = episode.uuid,
                title = episode.title,
                duration = episode.duration,
                playedUpTo = episode.playedUpTo,
                podcastUuid = episode.podcastUuid,
                artworkUrl = episode.imageUrl,
            )
            is UserEpisode -> User(
                uuid = episode.uuid,
                title = episode.title,
                duration = episode.duration,
                playedUpTo = episode.playedUpTo,
                artworkUrl = episode.artworkUrl,
                tintColorIndex = episode.tintColorIndex,
            )
        }
    }
}
