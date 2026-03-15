package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.PodcastListBadgeType

enum class BadgeType(
    internal val persistedInt: Int,
    val serverId: Int,
    @StringRes val labelId: Int,
    val eventHorizonValue: PodcastListBadgeType,
) {
    OFF(
        persistedInt = 0,
        serverId = 0,
        labelId = R.string.podcasts_badges_off,
        eventHorizonValue = PodcastListBadgeType.Off,
    ),

    LATEST_EPISODE(
        persistedInt = 1,
        serverId = 1,
        labelId = R.string.podcasts_badges_only_latest_episode,
        eventHorizonValue = PodcastListBadgeType.OnlyLatestEpisode,
    ),

    ALL_UNFINISHED(
        persistedInt = 2,
        serverId = 2,
        labelId = R.string.podcasts_badges_all_unfinished,
        eventHorizonValue = PodcastListBadgeType.UnfinishedEpisodes,
    ),
    ;

    val analyticsValue get() = eventHorizonValue.toString()

    companion object {
        val defaultValue = OFF

        fun fromPersistedInt(value: Int): BadgeType = BadgeType.values().find { it.persistedInt == value }
            ?: run {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Unknown persisted int for badge type: $value")
                defaultValue
            }

        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: defaultValue
    }
}
