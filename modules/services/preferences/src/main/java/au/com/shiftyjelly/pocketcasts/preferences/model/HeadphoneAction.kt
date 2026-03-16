package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import com.automattic.eventhorizon.HeadphoneActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HeadphoneAction(
    val serverId: Int,
    val analyticsValue: HeadphoneActionType,
) {
    ADD_BOOKMARK(
        serverId = 0,
        analyticsValue = HeadphoneActionType.AddBookmark,
    ),
    SKIP_BACK(
        serverId = 1,
        analyticsValue = HeadphoneActionType.SkipBack,
    ),
    SKIP_FORWARD(
        serverId = 2,
        analyticsValue = HeadphoneActionType.SkipForward,
    ),
    NEXT_CHAPTER(
        serverId = 3,
        analyticsValue = HeadphoneActionType.NextChapter,
    ),
    PREVIOUS_CHAPTER(
        serverId = 4,
        analyticsValue = HeadphoneActionType.PreviousChapter,
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
    subscriptionFlow: StateFlow<Subscription?>,
) : UserSetting.PrefFromInt<HeadphoneAction>(
    sharedPrefKey = sharedPrefKey,
    defaultValue = defaultAction,
    sharedPrefs = sharedPrefs,
    fromInt = {
        val hasNoSubscription = subscriptionFlow.value == null
        val nextAction = HeadphoneAction.entries[it]
        if (nextAction == HeadphoneAction.ADD_BOOKMARK && hasNoSubscription) {
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
            subscriptionFlow.collect { _ ->
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
