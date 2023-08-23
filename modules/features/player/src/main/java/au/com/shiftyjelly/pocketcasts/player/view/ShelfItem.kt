package au.com.shiftyjelly.pocketcasts.player.view

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

object ShelfItems {
    val itemsList = buildList {
        add(ShelfItem.Effects)
        add(ShelfItem.Sleep)
        add(ShelfItem.Star)
        add(ShelfItem.Share)
        add(ShelfItem.Podcast)
        add(ShelfItem.Cast)
        add(ShelfItem.Played)
        if (FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)) {
            add(ShelfItem.Bookmark)
        }
        add(ShelfItem.Archive)
    }
    private val items = itemsList.associateBy { it.id }

    fun itemForId(id: String): ShelfItem? {
        return items[id]
    }
}

sealed class ShelfItem(
    val id: String,
    var title: (BaseEpisode?) -> Int,
    var iconRes: (BaseEpisode?) -> Int,
    val shownWhen: Shown,
    val tier: SubscriptionTier = SubscriptionTier.NONE,
    val analyticsValue: String,
    @StringRes val subtitle: Int? = null
) {
    sealed class Shown {
        object Always : Shown()
        object EpisodeOnly : Shown()
        object UserEpisodeOnly : Shown()
    }

    object Effects : ShelfItem(
        id = "effects",
        title = { LR.string.podcast_playback_effects },
        iconRes = { IR.drawable.ic_effects_off },
        shownWhen = Shown.Always,
        analyticsValue = "playback_effects",
    )

    object Sleep : ShelfItem(
        id = "sleep",
        title = { LR.string.player_sleep_timer },
        iconRes = { R.drawable.ic_sleep },
        shownWhen = Shown.Always,
        analyticsValue = "sleep_timer"
    )

    object Star : ShelfItem(
        id = "star",
        title = { if (it is PodcastEpisode && it.isStarred) LR.string.unstar_episode else LR.string.star_episode },
        subtitle = LR.string.player_actions_hidden_for_custom,
        iconRes = { if (it is PodcastEpisode && it.isStarred) IR.drawable.ic_star_filled else IR.drawable.ic_star },
        shownWhen = Shown.EpisodeOnly,
        analyticsValue = "star_episode",
    )

    object Share : ShelfItem(
        id = "share",
        title = { LR.string.podcast_share_episode },
        subtitle = LR.string.player_actions_hidden_for_custom,
        iconRes = { IR.drawable.ic_share },
        shownWhen = Shown.EpisodeOnly,
        analyticsValue = "share_episode"
    )

    object Podcast : ShelfItem(
        id = "podcast",
        title = { if (it is UserEpisode) LR.string.go_to_files else LR.string.go_to_podcast },
        iconRes = { R.drawable.ic_arrow_goto },
        shownWhen = Shown.Always,
        analyticsValue = "go_to_podcast"
    )

    object Cast : ShelfItem(
        id = "cast",
        title = { LR.string.chromecast },
        iconRes = { com.google.android.gms.cast.framework.R.drawable.quantum_ic_cast_connected_white_24 },
        shownWhen = Shown.Always,
        analyticsValue = "chromecast"
    )

    object Played : ShelfItem(
        id = "played",
        title = { LR.string.mark_as_played },
        iconRes = { R.drawable.ic_markasplayed },
        shownWhen = Shown.Always,
        analyticsValue = "mark_as_played"
    )

    object Bookmark : ShelfItem(
        id = "bookmark",
        title = { LR.string.add_bookmark },
        iconRes = { IR.drawable.ic_bookmark },
        shownWhen = Shown.Always,
        tier = SubscriptionTier.PATRON,
        analyticsValue = "add_bookmark"
    )

    object Archive : ShelfItem(
        id = "archive",
        title = { if (it is UserEpisode) LR.string.delete else LR.string.archive },
        subtitle = LR.string.player_actions_show_as_delete_for_custom,
        iconRes = { if (it is UserEpisode) VR.drawable.ic_delete else IR.drawable.ic_archive },
        shownWhen = Shown.Always,
        analyticsValue = "archive"
    )

    object Download : ShelfItem(
        id = "download",
        title = {
            if (it?.isDownloaded == true) {
                LR.string.delete_download
            } else if (it?.isDownloading == true) {
                LR.string.cancel_download
            } else {
                LR.string.download
            }
        },
        iconRes = {
            if (it?.isDownloaded == true) {
                VR.drawable.ic_delete
            } else if (it?.episodeStatus == EpisodeStatusEnum.DOWNLOADING || it?.episodeStatus == EpisodeStatusEnum.QUEUED) {
                IR.drawable.ic_cancel
            } else {
                IR.drawable.ic_download
            }
        },
        shownWhen = Shown.Always,
        tier = SubscriptionTier.NONE,
        analyticsValue = "download"
    )
}
