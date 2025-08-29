package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.ad.AdBanner
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

internal class BannerAdAdapter(
    private val themeType: Theme.ThemeType,
    private val onAdClick: (BlazeAd) -> Unit,
    private val onAdOptionsClick: (BlazeAd) -> Unit,
    private val onAdImpression: (BlazeAd) -> Unit,
) : ListAdapter<BlazeAd, RecyclerView.ViewHolder>(BannerAdDiffCallback) {
    override fun getItemViewType(position: Int): Int {
        return AdapterViewTypeIds.BANNER_AD_ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BannerAdViewHolder(
            composeView = ComposeView(parent.context),
            themeType = themeType,
            onAdClick = onAdClick,
            onAdOptionsClick = onAdOptionsClick,
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as BannerAdViewHolder).bind(currentList[position])
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        val ad = requireNotNull((holder as BannerAdViewHolder).ad) {
            "Banner ad view attached without ad data"
        }
        onAdImpression(ad)
    }
}

private object BannerAdDiffCallback : DiffUtil.ItemCallback<BlazeAd>() {
    override fun areItemsTheSame(oldItem: BlazeAd, newItem: BlazeAd) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: BlazeAd, newItem: BlazeAd) = oldItem == newItem
}

private class BannerAdViewHolder(
    private val composeView: ComposeView,
    private val themeType: Theme.ThemeType,
    private val onAdClick: (BlazeAd) -> Unit,
    private val onAdOptionsClick: (BlazeAd) -> Unit,
) : RecyclerView.ViewHolder(composeView) {
    var ad: BlazeAd? = null

    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    fun bind(ad: BlazeAd) {
        this.ad = ad
        composeView.setContent {
            AppTheme(themeType) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    AdBanner(
                        ad = ad,
                        colors = rememberAdColors().bannerAd,
                        onAdClick = { onAdClick(ad) },
                        onOptionsClick = { onAdOptionsClick(ad) },
                    )
                }
            }
        }
    }
}
