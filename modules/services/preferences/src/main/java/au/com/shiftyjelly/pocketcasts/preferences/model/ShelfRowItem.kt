package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

interface ShelfRowItem

enum class ShelfItem(
    val id: String,
    val titleId: (BaseEpisode?) -> Int,
    val subtitleId: (BaseEpisode?) -> Int? = { null },
    val iconId: (BaseEpisode?) -> Int,
    val showIf: (BaseEpisode?) -> Boolean = { true },
    val analyticsValue: String,
) : ShelfRowItem {
    Effects(
        id = "effects",
        titleId = { LR.string.podcast_playback_effects },
        iconId = { IR.drawable.ic_effects_off },
        analyticsValue = "playback_effects",
    ),
    Sleep(
        id = "sleep",
        titleId = { LR.string.player_sleep_timer },
        iconId = { IR.drawable.ic_sleep },
        analyticsValue = "sleep_timer",
    ),
    Star(
        id = "star",
        titleId = { if (it is PodcastEpisode && it.isStarred) LR.string.unstar_episode else LR.string.star_episode },
        subtitleId = { episode -> LR.string.player_actions_hidden_for_custom.takeIf { episode is UserEpisode } },
        iconId = { if (it is PodcastEpisode && it.isStarred) IR.drawable.ic_star_filled else IR.drawable.ic_star },
        showIf = { it is PodcastEpisode },
        analyticsValue = "star_episode",
    ),
    Share(
        id = "share",
        titleId = { LR.string.podcast_share_episode },
        subtitleId = { episode -> LR.string.player_actions_hidden_for_custom.takeIf { episode is UserEpisode } },
        iconId = { IR.drawable.ic_share },
        showIf = { it is PodcastEpisode },
        analyticsValue = "share_episode",
    ),
    AddToPlaylist(
        id = "add_to_playlist",
        titleId = { LR.string.add_to_playlist_description },
        showIf = { it is PodcastEpisode && FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true) },
        iconId = { IR.drawable.ic_playlist_add_episode },
        analyticsValue = "add_to_playlist",
    ),
    Download(
        id = "download",
        titleId = {
            when {
                it is PodcastEpisode && (it.isDownloading || it.isQueued) -> LR.string.episode_downloading
                it is PodcastEpisode && it.isDownloaded -> LR.string.remove_downloaded_file
                else -> LR.string.download
            }
        },
        iconId = {
            when {
                it is PodcastEpisode && (it.isDownloading || it.isQueued) -> IR.drawable.ic_download
                it is PodcastEpisode && it.isDownloaded -> IR.drawable.ic_downloaded_24dp
                else -> IR.drawable.ic_download
            }
        },
        subtitleId = { episode -> LR.string.player_actions_hidden_for_custom.takeIf { episode is UserEpisode } },
        showIf = { it is PodcastEpisode },
        analyticsValue = "download",
    ),
    Transcript(
        id = "transcript",
        titleId = { LR.string.transcript },
        iconId = { IR.drawable.ic_transcript_24 },
        showIf = { it is PodcastEpisode },
        analyticsValue = "transcript",
    ),
    Podcast(
        id = "podcast",
        titleId = { if (it is UserEpisode) LR.string.go_to_files else LR.string.go_to_podcast },
        iconId = { IR.drawable.ic_arrow_goto },
        analyticsValue = "go_to_podcast",
    ),
    Bookmark(
        id = "bookmark",
        titleId = { LR.string.add_bookmark },
        iconId = { IR.drawable.ic_bookmark },
        analyticsValue = "add_bookmark",
    ),
    Cast(
        id = "cast",
        titleId = { LR.string.chromecast },
        iconId = { com.google.android.gms.cast.framework.R.drawable.quantum_ic_cast_connected_white_24 },
        analyticsValue = "chromecast",
    ),
    Played(
        id = "played",
        titleId = { LR.string.mark_as_played },
        iconId = { IR.drawable.ic_markasplayed },
        analyticsValue = "mark_as_played",
    ),
    Archive(
        id = "archive",
        titleId = { if (it is UserEpisode) LR.string.delete else LR.string.archive },
        subtitleId = { episode -> LR.string.player_actions_show_as_delete_for_custom.takeIf { episode is UserEpisode } },
        iconId = { if (it is UserEpisode) IR.drawable.ic_delete else IR.drawable.ic_archive },
        analyticsValue = "archive",
    ),
    ;

    // We can safely use the ID as server ID. Keeping it if need to make changes in the future.
    val serverId get() = id

    companion object {
        fun fromId(id: String) = entries.find { it.id == id }

        fun fromServerId(id: String) = entries.find { it.serverId == id }
    }
}

data class ShelfTitle(@StringRes val title: Int) : ShelfRowItem
