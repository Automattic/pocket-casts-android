package au.com.shiftyjelly.pocketcasts.preferences

import android.content.SharedPreferences
import androidx.annotation.CallSuper
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
    // lazy because the class needs to initialize before calling get()
    val flow by lazy { _flow }

    // external callers should use the flow.value to get the current value or, even
    // better, use the flow itself to observe changes.
    protected abstract fun get(): T

    @CallSuper
    open fun set(value: T) {
        _flow.value = value
    }

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

            super.set(value)
        }

        override fun get(): Boolean = sharedPrefs.getBoolean(sharedPrefKey, defaultValue)
    }

    class PrefFromInt<T>(
        sharedPrefKey: String,
        private val defaultValue: T,
        sharedPrefs: SharedPreferences,
        private val fromInt: (Int) -> T,
        private val toInt: (T) -> Int,
    ) : UserSetting<T>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {
        override fun get(): T {
            val persistedInt = sharedPrefs.getInt(sharedPrefKey, toInt(defaultValue))
            return fromInt(persistedInt)
        }

        override fun set(value: T) {
            val intValue = toInt(value)
            sharedPrefs.edit().run {
                putInt(sharedPrefKey, intValue)
                apply()
            }
            super.set(value)
        }
    }

    open class PrefFromString<T>(
        sharedPrefKey: String,
        private val defaultValue: T,
        sharedPrefs: SharedPreferences,
        private val fromString: (String) -> T,
        private val toString: (T) -> String,
    ) : UserSetting<T>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {
        private val defaultString = this.toString(defaultValue)

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
            super.set(value)
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
            fromString = { value ->
                try {
                    val valueInt = Integer.parseInt(value)
                    if (valueInt <= 0) defaultValue else valueInt
                } catch (nfe: NumberFormatException) {
                    defaultValue
                }
            },
            toString = { value ->
                val intValue = if (value <= 0) defaultValue else value
                intValue.toString()
            }
        )
    }

    class PrefListFromString<T>(
        sharedPrefKey: String,
        sharedPrefs: SharedPreferences,
        private val defaultValue: List<T>,
        private val fromString: (String) -> T?,
        private val toString: (T) -> String,
    ) : UserSetting<List<T>>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {

        override fun get(): List<T> {
            val strValue = sharedPrefs.getString(sharedPrefKey, "")
            return if (strValue.isNullOrEmpty()) {
                defaultValue
            } else {
                val commaSeparatedString = strValue.split(",")
                commaSeparatedString.mapNotNull(fromString)
            }
        }

        override fun set(value: List<T>) {
            sharedPrefs.edit().run {
                val commaSeparatedString = value.joinToString(
                    separator = ",",
                    transform = toString
                )
                putString(sharedPrefKey, commaSeparatedString)
                apply()
            }
            super.set(value)
        }
    }
}
