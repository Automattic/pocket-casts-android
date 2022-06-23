package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.content.SharedPreferences

// Android 10 added nullability to shared preferences except that its wrong for Strings.
// I'm rewriting it here to use optionals properly
fun SharedPreferences.getString(key: String): String? {
    return getString(key, null)
}

fun SharedPreferences.getStringSet(key: String): MutableSet<String>? {
    return getStringSet(key, null)
}
