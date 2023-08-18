package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    subscriptionStatusFlow: StateFlow<SubscriptionStatus?>,
) : UserSetting.PrefFromInt<HeadphoneAction>(
    sharedPrefKey = sharedPrefKey,
    defaultValue = defaultAction,
    sharedPrefs = sharedPrefs,
    fromInt = {
        val isAddBookmarkEnabled =
            FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) && (subscriptionStatusFlow.value as? SubscriptionStatus.Paid)?.tier == SubscriptionTier.PATRON
        val nextAction = HeadphoneAction.values()[it]
        if (nextAction == HeadphoneAction.ADD_BOOKMARK && !isAddBookmarkEnabled) {
            defaultAction
        } else {
            nextAction
        }
    },
    toInt = { it.ordinal }
) {

    // Even though this coroutine scope never gets cancelled explicitly, it should
    // get cancelled if/when the HeadphoneActionUserSetting is garbage collected.
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    init {
        // We're never cancelling this coroutine, but that should be fine
        // since it is doing so little work (also, we don't have a relevant lifecycle
        // to let us know when to cancel it).
        coroutineScope.launch {
            subscriptionStatusFlow.collect { _ ->
                // Update the flow value because the subscription status has changed, so
                // some actions (like bookmarks) might now be/not-be valid. We're using
                // _flow.value here instead of set() because we don't want to persist a
                // new value to shared prefs unless the user explicitly changes the
                // setting.
                _flow.value = get()
            }
        }
    }
}
