package au.com.shiftyjelly.pocketcasts.analytics

import android.content.SharedPreferences
import timber.log.Timber
import java.util.UUID

abstract class IdentifyingTracker(
    private val preferences: SharedPreferences
) : Tracker {
    private var anonymousID: String? = null // do not access this variable directly. Use methods.
    protected abstract val anonIdPrefKey: String?
    var userId: String? = null

    abstract override fun track(event: AnalyticsEvent, properties: Map<String, Any>)
    abstract override fun refreshMetadata()

    abstract override fun flush()
    override fun clearAllData() {
        clearAnonID()
        userId = null
    }

    protected fun clearAnonID() {
        anonymousID = null
        if (preferences.contains(anonIdPrefKey)) {
            val editor = preferences.edit()
            editor.remove(anonIdPrefKey)
            editor.apply()
        }
    }

    protected val anonID: String?
        get() {
            if (anonymousID == null) {
                anonymousID = preferences.getString(anonIdPrefKey, null)
            }
            return anonymousID
        }

    protected fun generateNewAnonID(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        Timber.d("\uD83D\uDD35 New anonID generated in " + this.javaClass.simpleName + ": " + uuid)
        val editor = preferences.edit()
        editor.putString(anonIdPrefKey, uuid)
        editor.apply()
        anonymousID = uuid
        return uuid
    }
}
