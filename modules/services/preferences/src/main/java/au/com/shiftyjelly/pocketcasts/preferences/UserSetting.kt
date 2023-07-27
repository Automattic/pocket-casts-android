package au.com.shiftyjelly.pocketcasts.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

abstract class UserSetting<T>(
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

//    abstract class PrefFromInt<T>(
//        sharedPrefKey: String,
//        private val defaultValue: T,
//        sharedPrefs: SharedPreferences,
//    ) : UserSetting<T>(
//        sharedPrefKey = sharedPrefKey,
//        sharedPrefs = sharedPrefs,
//    ) {
//        protected abstract fun fromInt(value: Int): T
//        protected abstract fun toInt(value: T): Int
//
//        override fun get(): T {
//            val persistedInt = sharedPrefs.getInt(sharedPrefKey, toInt(defaultValue))
//            return fromInt(persistedInt)
//        }
//
//        override fun set(value: T) {
//            val intValue = toInt(value)
//            sharedPrefs.edit().run {
//                putInt(sharedPrefKey, intValue)
//                apply()
//            }
//            updateFlow(value)
//        }
//    }

    abstract class PrefFromString<T>(
        sharedPrefKey: String,
        protected val defaultValue: T,
        sharedPrefs: SharedPreferences,
    ) : UserSetting<T>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {
        private val defaultString = this.toString(defaultValue)

        protected abstract fun fromString(value: String): T
        protected abstract fun toString(value: T): String

        override fun get(): T {

            val persistedString = sharedPrefs.getString(sharedPrefKey, defaultString) ?: defaultString
            return fromString(persistedString)
        }

        override fun set(value: T) {
            val stringValue = toString(value)
            sharedPrefs.edit().run {
                putString(sharedPrefKey, stringValue)
                apply()
            }
            updateFlow(value)
        }

        // This stores an the skip value Int as a String in shared preferences.
        class SkipAmount(
            sharedPrefKey: String,
            defaultValue: Int,
            sharedPrefs: SharedPreferences,
        ) : PrefFromString<Int>(
            sharedPrefKey = sharedPrefKey,
            defaultValue = defaultValue,
            sharedPrefs = sharedPrefs,
        ) {
            override fun fromString(value: String): Int =
                try {
                    val valueInt = Integer.parseInt(value)
                    if (valueInt <= 0) defaultValue else valueInt
                } catch (nfe: NumberFormatException) {
                    defaultValue
                }

            override fun toString(value: Int): String {
                val intValue = if (value <= 0) defaultValue else value
                return intValue.toString()
            }
        }
    }
}
