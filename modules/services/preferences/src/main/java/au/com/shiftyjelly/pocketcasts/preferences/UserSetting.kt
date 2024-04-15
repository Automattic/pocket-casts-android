package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class UserSetting<T>(
    val sharedPrefKey: String,
    protected val sharedPrefs: SharedPreferences,
) {

    private val modifiedAtKey = "${sharedPrefKey}ModifiedAt"

    private fun getModifiedAtServerString(): String? = sharedPrefs.getString(modifiedAtKey, null)

    val modifiedAt get(): Instant? = runCatching {
        Instant.parse(getModifiedAtServerString())
    }.getOrNull()

    /**
     * Returns the value to sync. If sync is not needed or the modification timestamp is unknown
     * it provides [Instant.EPOCH] plus one millisecond as the modification timestamp.
     */
    fun <U> getSyncSetting(f: (T, Instant) -> U): U {
        return f(value, modifiedAt ?: DefaultFallbackTimestamp)
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
    // listen to the flow for changes.
    protected abstract fun get(): T

    val value: T
        get() = flow.value

    protected abstract fun persist(value: T, commit: Boolean)

    open fun set(
        value: T,
        updateModifiedAt: Boolean,
        commit: Boolean = false,
        clock: Clock = Clock.systemUTC(),
    ) {
        persist(value, commit)
        _flow.value = get()
        val modifiedAt = if (updateModifiedAt) Instant.now(clock) else null
        updateModifiedAtServerString(modifiedAt)
    }

    private fun updateModifiedAtServerString(modifiedAt: Instant?) {
        if (modifiedAt != null) {
            sharedPrefs.edit().run {
                putString(modifiedAtKey, modifiedAt.toString())
                apply()
            }
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
        override fun persist(value: T, commit: Boolean) = Unit
        override fun set(value: T, updateModifiedAt: Boolean, commit: Boolean, clock: Clock) = Unit
    }

    companion object {
        // We use EPOCH +1 second as a default timestamp for updates because initial values of when app is installed are null.
        // This means that if a user syncs settings that were set before we started tracking timestamps
        // they would not update on a new device because we update settings only if the local timestamp is before remote timestamp.
        val DefaultFallbackTimestamp: Instant = Instant.EPOCH.plusSeconds(1)
    }
}
