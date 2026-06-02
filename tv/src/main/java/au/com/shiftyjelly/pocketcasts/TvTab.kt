package au.com.shiftyjelly.pocketcasts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class TvTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int? = null,
) {
    data object Podcasts : TvTab(labelRes = LR.string.podcasts, iconRes = IR.drawable.ic_podcasts)
    data object Discover : TvTab(labelRes = LR.string.discover, iconRes = IR.drawable.ic_discover)
    data object Search : TvTab(labelRes = LR.string.search, iconRes = IR.drawable.ic_search)

    companion object {
        val entries: List<TvTab> = listOf(Podcasts, Discover, Search)
    }
}
