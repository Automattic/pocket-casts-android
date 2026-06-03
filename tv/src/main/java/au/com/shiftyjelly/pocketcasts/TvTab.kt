package au.com.shiftyjelly.pocketcasts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class TvTab(@StringRes val contentDescriptionRes: Int) {

    sealed class TextTab(
        @StringRes val labelRes: Int,
    ) : TvTab(contentDescriptionRes = labelRes)

    sealed class IconTab(
        @StringRes contentDescriptionRes: Int,
        @DrawableRes val iconRes: Int,
    ) : TvTab(contentDescriptionRes)

    sealed class TextWithIconTab(
        @StringRes val labelRes: Int,
        @DrawableRes val iconRes: Int,
    ) : TvTab(contentDescriptionRes = labelRes)

    data object Home : TextTab(labelRes = LR.string.home)
    data object YourPodcasts : TextTab(labelRes = LR.string.your_podcasts)
    data object Playlists : TextTab(labelRes = LR.string.playlists)
    data object UpNext : TextTab(labelRes = LR.string.up_next)
    data object Search : IconTab(contentDescriptionRes = LR.string.search, iconRes = IR.drawable.ic_search)

    companion object {
        val entries: List<TvTab> = listOf(Home, YourPodcasts, Playlists, UpNext, Search)
    }
}
