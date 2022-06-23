package au.com.shiftyjelly.pocketcasts.models.to

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R

data class Bundle(
    val uuid: String?,
    val bundleUrl: String?,
    val paymentUrl: String?,
    val description: String?,
    val podcastUuid: String? = null,
    val paidType: BundlePaidType? = null
)

enum class BundlePaidType(@StringRes val stringRes: Int) {
    NOADS(R.string.supporter_paid_type_noads),
    EXCLUSIVE(R.string.supporter_paid_type_exclusive),
    NOADSEXCLUSIVE(R.string.supporter_paid_type_noads_exclusive),
    PREMIUM(R.string.supporter_paid_type_premium),
    NOADSPURCHASE(R.string.supporter_paid_type_noads_purchase),
    PREMIUMPURCHASE(R.string.supporter_paid_type_premium_purchase)
}
