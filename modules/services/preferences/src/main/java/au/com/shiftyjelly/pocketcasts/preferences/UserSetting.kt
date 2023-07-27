package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

sealed class UserSetting<T>(
    protected val sharedPrefKey: String,
    protected val sharedPrefs: SharedPreferences,
    private val syncable: Boolean,
) {

    var needsSync: Boolean
        get() = sharedPrefs.getBoolean("${sharedPrefKey}NeedsSync", false)
        set(value) {
            if (syncable) {
                sharedPrefs.edit().run {
                    putBoolean("${sharedPrefKey}NeedsSync", value)
                    apply()
                }
            } else {
                throw IllegalStateException("Cannot set needsSync on a UserSetting ($sharedPrefKey) that is not syncable")
            }
        }

    fun getSyncValue(): T? =
        if (needsSync) get() else null

    private val _flow by lazy { MutableStateFlow(get()) }
    val flow by lazy { _flow }

    // external callers should use the flow.value to get the current value or, even
    // better, use the flow itself to observe changes.
    protected abstract fun get(): T

    abstract val defaultValue: T

    protected fun updateFlow(value: T) {
        _flow.value = value
    }

    abstract fun set(value: T, now: Boolean = false)

//    sealed class Boolean(sharedPrefKey: String): UserSetting<Boolean> {
//
//    }

//    sealed class String(sharedPrefKey: String): UserSetting<String> {
//
//    }

//    sealed class Int(sharedPrefKey: String): UserSetting<Int> {
//
//    }

    // This stores an Int preference as a String and only exists for legacy
    // reasons. No new preferences should use this class.
    open class IntFromString(
        sharedPrefKey: String,
        override val defaultValue: Int,
        private val allowNegative: Boolean = true,
        sharedPrefs: SharedPreferences,
        syncable: Boolean,
    ) : UserSetting<Int>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
        syncable = syncable
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

        @SuppressLint("ApplySharedPref")
        override fun set(value: Int, now: Boolean) {
            val adjustedValue = if (value <= 0 && !allowNegative) defaultValue else value

            val editor = sharedPrefs.edit()
            editor.putString(sharedPrefKey, adjustedValue.toString())
            if (now) {
                editor.commit()
            } else {
                editor.apply()
            }

            updateFlow(value)
        }
    }
}
