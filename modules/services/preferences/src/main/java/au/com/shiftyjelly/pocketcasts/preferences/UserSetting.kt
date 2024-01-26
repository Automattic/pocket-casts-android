package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class UserSetting<T>(
    val sharedPrefKey: String,
    protected val sharedPrefs: SharedPreferences,
) {

    private val modifiedAtKey = "${sharedPrefKey}ModifiedAt"

    private fun getModifiedAtServerString(): String? = sharedPrefs.getString(modifiedAtKey, null)

    fun getModifiedAt(): Instant? = tryOrNull {
        getModifiedAtServerString()?.let { Instant.parse(it) }
    }

    // Returns the value to sync if sync is needed. Returns null if sync is not needed.
    fun <U> getSyncSetting(f: (T, String) -> U): U? {
        val modifiedAtServerString = getModifiedAtServerString()
        // Only need to sync if modifiedAtServerString is not null
        return if (modifiedAtServerString != null) {
            f(value, modifiedAtServerString)
        } else {
            null
        }
    }

    // Returns the value to sync if sync is needed. Returns null if sync is not needed.
    @Deprecated("This can be removed when Feature.SETTINGS_SYNC flag is removed")
    fun getSyncValue(): T? {
        val needsSync = getModifiedAtServerString() != null
        return if (needsSync) value else null
    }

    // These are lazy because (1) the class needs to initialize before calling get() and
    // (2) we don't want to get the current value from SharedPreferences for every
    // setting immediately on app startup.
    protected val _flow by lazy { MutableStateFlow(get()) }
    val flow: StateFlow<T> by lazy { _flow }

    // External callers should use [value] to get the current value if they can't
    // listen to the flow for changes. This method is solely to be used to intitialize
    // the flow.
    protected abstract fun get(): T

    val value: T
        get() = flow.value

    protected abstract fun persist(value: T, commit: Boolean)

    open fun set(value: T, commit: Boolean = false, needsSync: Boolean) {
        persist(value, commit)
        _flow.value = value
        val modifiedAt = if (needsSync) Instant.now().toString() else null
        updateModifiedAtServerString(modifiedAt)
    }

    // A null parameter reflects a setting that does not need to be synced
    private fun updateModifiedAtServerString(modifiedAt: String?) {
        if (modifiedAt != null) {
            val parsable = tryOrNull { Instant.parse(modifiedAt) } != null
            if (!parsable) {
                // Only persist the string if it is parsable
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Cannot set invalid modified at server string: $modifiedAt")
                return
            }
        }

        sharedPrefs.edit().run {
            putString(modifiedAtKey, modifiedAt)
            apply()
        }
    }

    class BoolPref(
        sharedPrefKey: String,
        private val defaultValue: Boolean,
        sharedPrefs: SharedPreferences,
    ) : UserSetting<Boolean>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {

        @SuppressLint("ApplySharedPref")
        override fun persist(value: Boolean, commit: Boolean) {
            sharedPrefs.edit().run {
                putBoolean(sharedPrefKey, value)
                if (commit) {
                    commit()
                } else {
                    apply()
                }
            }
        }

        override fun get(): Boolean = sharedPrefs.getBoolean(sharedPrefKey, defaultValue)
    }

    class IntPref(
        sharedPrefKey: String,
        defaultValue: Int,
        sharedPrefs: SharedPreferences,
    ) : PrefFromInt<Int>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromInt = { it },
        toInt = { it },
    )

    class StringPref(
        sharedPrefKey: String,
        defaultValue: String,
        sharedPrefs: SharedPreferences,
    ) : PrefFromString<String>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromString = { it },
        toString = { it },
    )

    // This persists the parameterized object as an Int in shared preferences.
    open class PrefFromInt<T>(
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

        @SuppressLint("ApplySharedPref")
        override fun persist(value: T, commit: Boolean) {
            val intValue = toInt(value)
            sharedPrefs.edit().run {
                putInt(sharedPrefKey, intValue)
                if (commit) {
                    commit()
                } else {
                    apply()
                }
            }
        }
    }

    // This persists the parameterized object as a Float in shared preferences.
    open class PrefFromFloat<T>(
        sharedPrefKey: String,
        private val defaultValue: T,
        sharedPrefs: SharedPreferences,
        private val fromFloat: (Float) -> T,
        private val toFloat: (T) -> Float,
    ) : UserSetting<T>(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = sharedPrefs,
    ) {
        override fun get(): T {
            val persistedInt = sharedPrefs.getFloat(sharedPrefKey, toFloat(defaultValue))
            return fromFloat(persistedInt)
        }

        @SuppressLint("ApplySharedPref")
        override fun persist(value: T, commit: Boolean) {
            val floatValue = toFloat(value)
            sharedPrefs.edit().run {
                putFloat(sharedPrefKey, floatValue)
                if (commit) {
                    commit()
                } else {
                    apply()
                }
            }
        }
    }

    // This persists the parameterized object as a String in shared preferences.
    open class PrefFromString<T>(
        sharedPrefKey: String,
        defaultValue: T,
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

        @SuppressLint("ApplySharedPref")
        override fun persist(value: T, commit: Boolean) {
            val stringValue = toString(value)
            sharedPrefs.edit().run {
                putString(sharedPrefKey, stringValue)
                if (commit) {
                    commit()
                } else {
                    apply()
                }
            }
        }
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

        @SuppressLint("ApplySharedPref")
        override fun persist(value: List<T>, commit: Boolean) {
            sharedPrefs.edit().run {
                val commaSeparatedString = value.joinToString(
                    separator = ",",
                    transform = toString,
                )
                putString(sharedPrefKey, commaSeparatedString)
                if (commit) {
                    commit()
                } else {
                    apply()
                }
            }
        }
    }

    // This stores the skip value Int as a String in shared preferences.
    class SkipAmountPref(
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
        },
    )

    // This manual mock is needed to avoid problems when accessing a lazily initialized UserSetting::flow
    // from a mocked Settings class
    class Mock<T>(
        private val initialValue: T,
        sharedPrefs: SharedPreferences,
    ) : UserSetting<T>(
        sharedPrefKey = "a_shared_pref_key",
        sharedPrefs = sharedPrefs,
    ) {
        override fun get(): T = initialValue
        override fun persist(value: T, commit: Boolean) {}
        override fun set(value: T, commit: Boolean, needsSync: Boolean) {}
    }
}

private inline fun <T> tryOrNull(f: () -> T): T? =
    try {
        f()
    } catch (e: Exception) {
        null
    }
