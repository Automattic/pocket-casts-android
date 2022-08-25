package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID

abstract class Tracker(@ApplicationContext private val appContext: Context) {
    private var anonymousID: String? = null // do not access this variable directly. Use methods.
    abstract val anonIdPrefKey: String?
    abstract fun track(event: AnalyticsEvent, properties: Map<String, *> = emptyMap<String, String>())
    abstract fun flush()
    open fun clearAllData() {
        // Reset the anon ID here
        clearAnonID()
    }

    abstract fun storeUsagePref()

    private fun clearAnonID() {
        anonymousID = null
        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        if (preferences.contains(anonIdPrefKey)) {
            val editor = preferences.edit()
            editor.remove(anonIdPrefKey)
            editor.apply()
        }
    }

    val anonID: String?
        get() {
            if (anonymousID == null) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
                anonymousID = preferences.getString(anonIdPrefKey, null)
            }
            return anonymousID
        }

    fun generateNewAnonID(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        Timber.d("New anonID generated in " + this.javaClass.simpleName + ": " + uuid)
        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val editor = preferences.edit()
        editor.putString(anonIdPrefKey, uuid)
        editor.apply()
        anonymousID = uuid
        return uuid
    }
}
