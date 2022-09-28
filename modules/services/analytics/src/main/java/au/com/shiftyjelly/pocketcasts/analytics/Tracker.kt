package au.com.shiftyjelly.pocketcasts.analytics

import android.content.SharedPreferences
import timber.log.Timber
import java.util.UUID

abstract class Tracker(
    private val preferences: SharedPreferences
) {
    private var anonymousID: String? = null // do not access this variable directly. Use methods.
    abstract val anonIdPrefKey: String?
    var userId: String? = null

    abstract fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap())
    abstract fun refreshMetadata()

    abstract fun flush()
    open fun clearAllData() {
        clearAnonID()
        userId = null
    }

    fun clearAnonID() {
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
}
