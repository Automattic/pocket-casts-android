package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting

enum class HeadphoneAction(val analyticsValue: String) {
    ADD_BOOKMARK("add_bookmark"),
    SKIP_BACK("skip_back"),
    SKIP_FORWARD("skip_forward"),
    NEXT_CHAPTER("next_chapter"),
    PREVIOUS_CHAPTER("previous_chapter"),
}

class HeadphoneActionUserSetting(
    sharedPrefKey: String,
    defaultAction: HeadphoneAction,
    sharedPrefs: SharedPreferences,
    subscriptionStatus: () -> SubscriptionStatus?
) : UserSetting.PrefFromInt<HeadphoneAction>(
    sharedPrefKey = sharedPrefKey,
    defaultValue = defaultAction,
    sharedPrefs = sharedPrefs,
    fromInt = {
        val isAddBookmarkEnabled =
            FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) && (subscriptionStatus() as? SubscriptionStatus.Paid)?.tier == SubscriptionTier.PATRON
        val nextAction = HeadphoneAction.values()[it]
        if (nextAction == HeadphoneAction.ADD_BOOKMARK && !isAddBookmarkEnabled) {
            defaultAction
        } else {
            nextAction
        }
    },
    toInt = { it.ordinal }
)
