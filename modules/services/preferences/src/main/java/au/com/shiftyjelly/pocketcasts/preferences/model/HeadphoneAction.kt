package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HeadphoneAction(
    val analyticsValue: String,
    val serverId: Int,
) {
    ADD_BOOKMARK(
        analyticsValue = "add_bookmark",
        serverId = 0,
    ),
    SKIP_BACK(
        analyticsValue = "skip_back",
        serverId = 1,
    ),
    SKIP_FORWARD(
        analyticsValue = "skip_forward",
        serverId = 2,
    ),
    NEXT_CHAPTER(
        analyticsValue = "next_chapter",
        serverId = 3,
    ),
    PREVIOUS_CHAPTER(
        analyticsValue = "previous_chapter",
        serverId = 4,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}

class HeadphoneActionUserSetting(
    sharedPrefKey: String,
    defaultAction: HeadphoneAction,
    sharedPrefs: SharedPreferences,
    subscriptionStatusFlow: StateFlow<SubscriptionStatus?>,
    bookmarkFeature: BookmarkFeatureControl,
) : UserSetting.PrefFromInt<HeadphoneAction>(
    sharedPrefKey = sharedPrefKey,
    defaultValue = defaultAction,
    sharedPrefs = sharedPrefs,
    fromInt = {
        val userTier = (subscriptionStatusFlow.value as? SubscriptionStatus.Paid)?.tier?.toUserTier()
            ?: UserTier.Free
        val nextAction = HeadphoneAction.entries[it]
        if (nextAction == HeadphoneAction.ADD_BOOKMARK && !bookmarkFeature.isAvailable(userTier)) {
            defaultAction
        } else {
            nextAction
        }
    },
    toInt = { it.ordinal },
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
