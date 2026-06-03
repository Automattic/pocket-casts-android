package au.com.shiftyjelly.pocketcasts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class TvTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int? = null,
) {
    data object Home : TvTab(labelRes = LR.string.home)
    data object YourPodcasts : TvTab(labelRes = LR.string.your_podcasts)
    data object Playlists : TvTab(labelRes = LR.string.playlists)
    data object UpNext : TvTab(labelRes = LR.string.up_next)
    data object Search : TvTab(labelRes = LR.string.search, iconRes = IR.drawable.ic_search)

    companion object {
        val entries: List<TvTab> = listOf(Home, YourPodcasts, Playlists, UpNext, Search)
    }
}
