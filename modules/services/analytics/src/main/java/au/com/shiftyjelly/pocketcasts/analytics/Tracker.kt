package au.com.shiftyjelly.pocketcasts.analytics

import android.content.SharedPreferences
import androidx.annotation.CallSuper
import au.com.shiftyjelly.pocketcasts.utils.minutes
import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import timber.log.Timber
import java.util.Date
import java.util.UUID

abstract class Tracker(
    private val preferences: SharedPreferences
) {
    private var anonymousID: String? = null // do not access this variable directly. Use methods.
    abstract val anonIdPrefKey: String?
    /* The date the last event was tracked, used to determine when to regenerate the anonID */
    private var lastEventDate: Date? = null
    private val anonIDInactivityTimeout: Long = 30.minutes()

    @CallSuper
    open fun track(event: AnalyticsEvent, properties: Map<String, *> = emptyMap<String, String>()) {
        regenerateAnonIDIfNeeded()
        /* Update the last event date so we can monitor the anonID timeout */
        lastEventDate = Date()
    }

    @CallSuper
    open fun refreshMetadata() {
        if (anonID == null) {
            generateNewAnonID()
        }
    }

    abstract fun flush()
    open fun clearAllData() {
        // Reset the anon ID here
        clearAnonID()
    }

    private fun clearAnonID() {
        anonymousID = null
        if (preferences.contains(anonIdPrefKey)) {
            val editor = preferences.edit()
            editor.remove(anonIdPrefKey)
            editor.apply()
        }
    }

    val anonID: String?
        get() {
            if (anonymousID == null) {
                anonymousID = preferences.getString(anonIdPrefKey, null)
            }
            return anonymousID
        }

    fun generateNewAnonID(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        Timber.d("\uD83D\uDD35 New anonID generated in " + this.javaClass.simpleName + ": " + uuid)
        val editor = preferences.edit()
        editor.putString(anonIdPrefKey, uuid)
        editor.apply()
        anonymousID = uuid
        return uuid
    }

    private fun regenerateAnonIDIfNeeded() {
        lastEventDate?.let {
            if (it.timeIntervalSinceNow() < anonIDInactivityTimeout) return
            generateNewAnonID()
        }
    }
}
