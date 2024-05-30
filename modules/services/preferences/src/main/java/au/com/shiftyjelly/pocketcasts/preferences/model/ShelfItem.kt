package au.com.shiftyjelly.pocketcasts.preferences.model

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class ShelfItem(
    val id: String,
    val titleId: (BaseEpisode?) -> Int,
    val subtitleId: (BaseEpisode?) -> Int? = { null },
    val iconId: (BaseEpisode?) -> Int,
    val showIf: (BaseEpisode?) -> Boolean = { true },
    val analyticsValue: String,
) {
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
    Podcast(
        id = "podcast",
        titleId = { if (it is UserEpisode) LR.string.go_to_files else LR.string.go_to_podcast },
        iconId = { IR.drawable.ic_arrow_goto },
        analyticsValue = "go_to_podcast",
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
    Bookmark(
        id = "bookmark",
        titleId = { LR.string.add_bookmark },
        iconId = { IR.drawable.ic_bookmark },
        analyticsValue = "add_bookmark",
    ),
    Download(
        id = "download",
        titleId = { LR.string.download },
        iconId = { IR.drawable.ic_download },
        showIf = { it is PodcastEpisode && !it.isDownloaded },
        analyticsValue = "download",
    ),
    Archive(
        id = "archive",
        titleId = { if (it is UserEpisode) LR.string.delete else LR.string.archive },
        subtitleId = { episode -> LR.string.player_actions_show_as_delete_for_custom.takeIf { episode is UserEpisode } },
        iconId = { if (it is UserEpisode) IR.drawable.ic_delete else IR.drawable.ic_archive },
        analyticsValue = "archive",
    ),
    Report(
        id = "report",
        titleId = { LR.string.report },
        subtitleId = { if (it is PodcastEpisode) LR.string.report_subtitle else LR.string.player_actions_hidden_for_custom },
        iconId = { IR.drawable.ic_flag },
        showIf = { it is PodcastEpisode },
        analyticsValue = "report",
    ),
    ;

    // We can safely use the ID as server ID. Keeping it if need to make changes in the future.
    val serverId get() = id

    companion object {
        fun fromId(id: String) = entries.find { it.id == id }

        fun fromServerId(id: String) = entries.find { it.serverId == id }
    }
}
