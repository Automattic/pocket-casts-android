package au.com.shiftyjelly.pocketcasts.models.to

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class MediaNotificationControls(@StringRes val controlName: Int) {

    companion object {
        val All
            get() = listOf(Archive, MarkAsPlayed, PlayNext, PlaybackSpeed, Star)

        const val MaxSelectedOptions = 2

        val MediaControlKeys
            get() = listOf(
                archive_key, markAsPlayed_key, play_next_key, playback_speed_key,
                star_key
            )
        const val archive_key = "default_media_control_archive"
        const val markAsPlayed_key = "default_media_control_mark_as_played"
        const val play_next_key = "default_media_control_play_next_key"
        const val playback_speed_key = "default_media_control_playback_speed_key"
        const val star_key = "default_media_control_star_key"
    }

    object Archive : MediaNotificationControls(LR.string.archive)

    object MarkAsPlayed : MediaNotificationControls(LR.string.mark_as_played)

    object PlayNext : MediaNotificationControls(LR.string.play_next)

    object PlaybackSpeed : MediaNotificationControls(LR.string.playback_speed)

    object Star : MediaNotificationControls(LR.string.star)

}