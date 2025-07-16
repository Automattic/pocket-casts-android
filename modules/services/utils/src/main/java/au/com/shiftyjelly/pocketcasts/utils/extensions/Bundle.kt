package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.os.Build
import android.os.Bundle
import java.io.Serializable

fun <T : Serializable> Bundle.getSerializableCompat(key: String, clazz: Class<T>): T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getSerializable(key, clazz)
} else {
    @Suppress("DEPRECATION")
    getSerializable(key)?.let { result ->
        if (clazz.isInstance(result)) {
            @Suppress("UNCHECKED_CAST")
            result as T
        } else {
            null
        }
    }
}
