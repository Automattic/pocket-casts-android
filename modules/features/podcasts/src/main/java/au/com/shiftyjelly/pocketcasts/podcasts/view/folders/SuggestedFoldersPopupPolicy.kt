package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class SuggestedFoldersPopupPolicy @Inject constructor(
    settings: Settings,
    private val clock: Clock,
) {
    private val timestampSetting = settings.suggestedFoldersDismissTimestamp
    private val countSetting = settings.suggestedFoldersDismissCount
    private val subscriptionsSetting = settings.cachedSubscription

    fun isEligibleForPopup(): Boolean {
        val subscription = subscriptionsSetting.value
        val currentCount = countSetting.value

        return when {
            subscription != null -> false
            currentCount == 0 -> true
            currentCount == 1 -> {
                timestampSetting.value
                    ?.plusMillis(7.days.inWholeMilliseconds)
                    ?.isBefore(clock.instant())
                    ?: true
            }
            else -> false
        }
    }

    fun markPolicyUsed() {
        timestampSetting.set(clock.instant(), updateModifiedAt = false)
        countSetting.set(countSetting.value + 1, updateModifiedAt = false)
    }
}
