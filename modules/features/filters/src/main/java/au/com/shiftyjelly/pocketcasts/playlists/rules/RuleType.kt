package au.com.shiftyjelly.pocketcasts.playlists.rules

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R

enum class RuleType(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
) {
    Podcasts(
        iconId = R.drawable.ic_podcasts,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.podcasts,
    ),
    EpisodeStatus(
        iconId = R.drawable.ic_filters_play,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_chip_episode_status,
    ),
    ReleaseDate(
        iconId = R.drawable.ic_calendar,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_release_date,
    ),
    EpisodeDuration(
        iconId = R.drawable.ic_filters_clock,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_duration,
    ),
    DownloadStatus(
        iconId = R.drawable.ic_profile_download,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_chip_download_status,
    ),
    MediaType(
        iconId = R.drawable.ic_headphone,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_chip_media_type,
    ),
    Starred(
        iconId = R.drawable.ic_star,
        titleId = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_chip_starred,
    ),
}
