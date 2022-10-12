package au.com.shiftyjelly.pocketcasts.player.view

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

object ShelfItems {
    val itemsList = listOf(ShelfItem.Effects, ShelfItem.Sleep, ShelfItem.Star, ShelfItem.Share, ShelfItem.Podcast, ShelfItem.Cast, ShelfItem.Played, ShelfItem.Archive)
    val items = itemsList.associateBy { it.id }

    fun itemForId(id: String): ShelfItem? {
        return items[id]
    }
}

sealed class ShelfItem(val id: String, var title: (Playable?) -> Int, var iconRes: (Playable?) -> Int, val shownWhen: Shown, @StringRes val subtitle: Int? = null) {
    sealed class Shown {
        object Always : Shown()
        object EpisodeOnly : Shown()
        object UserEpisodeOnly : Shown()
    }

    object Effects : ShelfItem(ShelfItemId.EFFECTS.value, { LR.string.podcast_playback_effects }, { R.drawable.ic_effects_off }, Shown.Always)

    object Sleep : ShelfItem(ShelfItemId.SLEEP.value, { LR.string.player_sleep_timer }, { R.drawable.ic_sleep }, Shown.Always)

    object Star : ShelfItem(
        ShelfItemId.STAR.value,
        { if (it is Episode && it.isStarred) LR.string.unstar_episode else LR.string.star_episode },
        { if (it is Episode && it.isStarred) IR.drawable.ic_star_filled else IR.drawable.ic_star },
        Shown.EpisodeOnly,
        LR.string.player_actions_hidden_for_custom
    )

    object Share : ShelfItem(ShelfItemId.SHARE.value, { LR.string.podcast_share_episode }, { IR.drawable.ic_share }, Shown.EpisodeOnly, LR.string.player_actions_hidden_for_custom)

    object Podcast : ShelfItem(
        ShelfItemId.PODCAST.value,
        { if (it is UserEpisode) LR.string.go_to_files else LR.string.go_to_podcast },
        { R.drawable.ic_arrow_goto },
        Shown.Always
    )

    object Cast : ShelfItem(ShelfItemId.CAST.value, { LR.string.chromecast }, { com.google.android.gms.cast.framework.R.drawable.quantum_ic_cast_connected_white_24 }, Shown.Always)

    object Played : ShelfItem(ShelfItemId.PLAYED.value, { LR.string.mark_as_played }, { R.drawable.ic_markasplayed }, Shown.Always)

    object Archive : ShelfItem(
        ShelfItemId.ARCHIVE.value,
        { if (it is UserEpisode) LR.string.delete else LR.string.archive },
        { if (it is UserEpisode) VR.drawable.ic_delete else IR.drawable.ic_archive },
        Shown.Always,
        LR.string.player_actions_show_as_delete_for_custom
    )

    object Download : ShelfItem(
        ShelfItemId.DOWNLOAD.value,
        {
            if (it?.isDownloaded == true) {
                LR.string.delete_download
            } else if (it?.isDownloading == true) {
                LR.string.cancel_download
            } else {
                LR.string.download
            }
        },
        {
            if (it?.isDownloaded == true) {
                VR.drawable.ic_delete
            } else if (it?.episodeStatus == EpisodeStatusEnum.DOWNLOADING || it?.episodeStatus == EpisodeStatusEnum.QUEUED) {
                IR.drawable.ic_cancel
            } else {
                IR.drawable.ic_download
            }
        },
        Shown.Always
    )

    enum class ShelfItemId(val value: String, val analyticsValue: String) {
        EFFECTS("effects", "playback_effects"),
        SLEEP("sleep", "sleep_timer"),
        STAR("star", "star_episode"),
        SHARE("share", "share_episode"),
        PODCAST("podcast", "go_to_podcast"),
        CAST("cast", "chromecast"),
        PLAYED("played", "mark_as_played"),
        ARCHIVE("archive", "archive"),
        DOWNLOAD("download", "download");

        companion object {
            fun fromId(id: String?) =
                ShelfItemId.values().find { it.value == id }
        }
    }
}
