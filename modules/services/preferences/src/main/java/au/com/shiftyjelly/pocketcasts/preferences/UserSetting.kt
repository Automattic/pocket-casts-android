package au.com.shiftyjelly.pocketcasts.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

sealed class UserSetting<T>(
    protected val sharedPrefKey: String,
    protected val sharedPrefs: SharedPreferences,
) {

    var needsSync: Boolean
        get() = sharedPrefs.getBoolean("${sharedPrefKey}NeedsSync", false)
        set(value) {
            sharedPrefs.edit().run {
                putBoolean("${sharedPrefKey}NeedsSync", value)
                apply()
            }
        }

    fun getSyncValue(): T? =
        if (needsSync) get() else null

    private val _flow by lazy { MutableStateFlow(get()) }
    val flow by lazy { _flow }

    // external callers should use the flow.value to get the current value or, even
    // better, use the flow itself to observe changes.
    protected abstract fun get(): T

    protected fun updateFlow(value: T) {
        _flow.value = value
    }

    abstract fun set(value: T)

    class BoolPref(
        sharedPrefKey: String,
        private val defaultValue: Boolean,
        sharedPrefs: SharedPreferences,
    ) : UserSetting<Boolean>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {

        override fun set(value: Boolean) {
            sharedPrefs.edit().run {
                putBoolean(sharedPrefKey, value)
                apply()
            }
            updateFlow(value)
        }

        override fun get(): Boolean = sharedPrefs.getBoolean(sharedPrefKey, defaultValue)
    }

    // This stores an Int preference as a String and only exists for legacy
    // reasons. No new preferences should use this class.
    class IntFromStringPref(
        sharedPrefKey: String,
        private val defaultValue: Int,
        private val allowNegative: Boolean = true,
        sharedPrefs: SharedPreferences,
    ) : UserSetting<Int>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {

        override fun get(): Int {
            val defaultAsString = defaultValue.toString()
            val value = sharedPrefs.getString(sharedPrefKey, defaultAsString)
                ?: defaultAsString
            return try {
                val valueInt = Integer.parseInt(value)
                if (valueInt <= 0) defaultValue else valueInt
            } catch (nfe: NumberFormatException) {
                defaultValue
            }
        }

        override fun set(value: Int) {
            val adjustedValue = if (value <= 0 && !allowNegative) defaultValue else value
            sharedPrefs.edit().run {
                putString(sharedPrefKey, adjustedValue.toString())
                apply()
            }
            updateFlow(value)
        }
    }
}
