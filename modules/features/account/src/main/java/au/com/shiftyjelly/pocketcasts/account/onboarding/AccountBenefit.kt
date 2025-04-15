package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R

internal enum class AccountBenefit(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val cardImage: Int,
    @DrawableRes val listIcon: Int,
    val analyticsValue: String,
) {
    Sync(
        title = R.string.account_benefit_sync_title,
        description = R.string.account_benefit_sync_description,
        cardImage = au.com.shiftyjelly.pocketcasts.images.R.drawable.account_benefit_sync,
        listIcon = au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_account_benefit_sync,
        analyticsValue = "sync",
    ),
    Backups(
        title = R.string.account_benefit_backups_title,
        description = R.string.account_benefit_backups_description,
        cardImage = au.com.shiftyjelly.pocketcasts.images.R.drawable.account_benefit_backups,
        listIcon = au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_account_benefit_backups,
        analyticsValue = "backups",
    ),
    Recommendations(
        title = R.string.account_benefit_recommendations_title,
        description = R.string.account_benefit_recommendations_description,
        cardImage = au.com.shiftyjelly.pocketcasts.images.R.drawable.account_benefit_recommendations,
        listIcon = au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_account_benefit_recommendations,
        analyticsValue = "recommendation",
    ),
}
