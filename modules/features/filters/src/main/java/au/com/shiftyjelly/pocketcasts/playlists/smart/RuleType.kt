package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class RuleType(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
    val analyticsValue: String,
) {
    Podcasts(
        iconId = IR.drawable.ic_podcasts,
        titleId = LR.string.podcasts,
        analyticsValue = "podcasts",
    ),
    EpisodeStatus(
        iconId = IR.drawable.ic_filters_play,
        titleId = LR.string.filters_chip_episode_status,
        analyticsValue = "episode_status",
    ),
    ReleaseDate(
        iconId = IR.drawable.ic_filters_calendar,
        titleId = LR.string.filters_release_date,
        analyticsValue = "release_date",
    ),
    EpisodeDuration(
        iconId = IR.drawable.ic_filters_clock,
        titleId = LR.string.filters_duration,
        analyticsValue = "episode_duration",
    ),
    DownloadStatus(
        iconId = IR.drawable.ic_profile_download,
        titleId = LR.string.filters_chip_download_status,
        analyticsValue = "download_status",
    ),
    MediaType(
        iconId = IR.drawable.ic_headphone,
        titleId = LR.string.filters_chip_media_type,
        analyticsValue = "media_type",
    ),
    Starred(
        iconId = IR.drawable.ic_star,
        titleId = LR.string.filters_chip_starred,
        analyticsValue = "starred",
    ),
}
