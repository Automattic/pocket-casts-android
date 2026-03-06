package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.automattic.eventhorizon.SmartRuleType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class RuleType(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    val eventHorizonValue: SmartRuleType,
) {
    Podcasts(
        iconId = IR.drawable.ic_podcasts,
        titleId = LR.string.podcasts,
        eventHorizonValue = SmartRuleType.Podcasts,
    ),
    EpisodeStatus(
        iconId = IR.drawable.ic_filters_play,
        titleId = LR.string.filters_chip_episode_status,
        eventHorizonValue = SmartRuleType.EpisodeStatus,
    ),
    ReleaseDate(
        iconId = IR.drawable.ic_filters_calendar,
        titleId = LR.string.filters_release_date,
        eventHorizonValue = SmartRuleType.ReleaseDate,
    ),
    EpisodeDuration(
        iconId = IR.drawable.ic_filters_clock,
        titleId = LR.string.filters_duration,
        eventHorizonValue = SmartRuleType.EpisodeDuration,
    ),
    DownloadStatus(
        iconId = IR.drawable.ic_profile_download,
        titleId = LR.string.filters_chip_download_status,
        eventHorizonValue = SmartRuleType.DownloadStatus,
    ),
    MediaType(
        iconId = IR.drawable.ic_headphone,
        titleId = LR.string.filters_chip_media_type,
        eventHorizonValue = SmartRuleType.MediaType,
    ),
    Starred(
        iconId = IR.drawable.ic_star,
        titleId = LR.string.filters_chip_starred,
        eventHorizonValue = SmartRuleType.Starred,
    ),
}
