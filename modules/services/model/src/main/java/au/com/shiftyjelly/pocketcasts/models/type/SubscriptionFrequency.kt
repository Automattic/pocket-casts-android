package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import com.squareup.moshi.JsonClass
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@JsonClass(generateAdapter = false)
enum class SubscriptionFrequency(val label: String, @StringRes val localisedLabelRes: Int) {
    NONE("none", LR.string.none),
    MONTHLY("monthly", LR.string.plus_monthly),
    YEARLY("yearly", LR.string.plus_yearly),
    ;
    override fun toString() = label
}
