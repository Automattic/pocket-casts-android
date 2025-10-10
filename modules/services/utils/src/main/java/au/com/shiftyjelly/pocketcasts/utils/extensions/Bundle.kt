package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import java.io.Serializable

fun Bundle.findBoolean(key: String) = withKey(key) { getBoolean(key) }
fun Bundle.requireBoolean(key: String) = requireKey(key, ::findBoolean)

fun Bundle.findByte(key: String) = withKey(key) { getByte(key) }
fun Bundle.requireByte(key: String) = requireKey(key, ::findByte)

fun Bundle.findChar(key: String) = withKey(key) { getChar(key) }
fun Bundle.requireChar(key: String) = requireKey(key, ::findChar)

fun Bundle.findDouble(key: String) = withKey(key) { getDouble(key) }
fun Bundle.requireDouble(key: String) = requireKey(key, ::findDouble)

fun Bundle.findFloat(key: String) = withKey(key) { getFloat(key) }
fun Bundle.requireFloat(key: String) = requireKey(key, ::findFloat)

fun Bundle.findInt(key: String) = withKey(key) { getInt(key) }
fun Bundle.requireInt(key: String) = requireKey(key, ::findInt)

fun Bundle.findLong(key: String) = withKey(key) { getLong(key) }
fun Bundle.requireLong(key: String) = requireKey(key, ::findLong)

fun Bundle.findShort(key: String) = withKey(key) { getShort(key) }
fun Bundle.requireShort(key: String) = requireKey(key, ::findShort)

fun Bundle.findString(key: String) = withKey(key) { getString(key) }
fun Bundle.requireString(key: String) = requireKey(key, ::findString)

fun Bundle.findBundle(key: String) = withKey(key) { getBundle(key) }
fun Bundle.requireBundle(key: String) = requireKey(key, ::findBundle)

inline fun <reified T : Serializable> Bundle.findSerializable(key: String) = withKey(key) { BundleCompat.getSerializable(this, key, T::class.java) }
inline fun <reified T : Serializable> Bundle.requireSerializable(key: String) = requireKey(key) { findSerializable<T>(it) }

inline fun <reified T : Parcelable> Bundle.findParcelable(key: String) = withKey(key) { BundleCompat.getParcelable(this, key, T::class.java) }
inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String) = requireKey(key) { findParcelable<T>(it) }

inline fun <reified T : Parcelable> Bundle.findParcelableList(key: String) = withKey(key) { BundleCompat.getParcelableArrayList(this, key, T::class.java) }
inline fun <reified T : Parcelable> Bundle.requireParcelableList(key: String) = requireKey(key) { findParcelableList<T>(it) }

@PublishedApi
internal fun <T : Any> Bundle.withKey(key: String, block: Bundle.() -> T?): T? {
    return if (containsKey(key)) block() else null
}

@PublishedApi
internal fun <T : Any> requireKey(key: String, block: (String) -> T?): T {
    return requireNotNull(block(key)) {
        "Missing value for key \"$key\""
    }
}
