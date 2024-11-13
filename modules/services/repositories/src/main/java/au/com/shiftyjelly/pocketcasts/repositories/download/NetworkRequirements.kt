package au.com.shiftyjelly.pocketcasts.repositories.download

import android.text.TextUtils
import androidx.work.NetworkType

data class NetworkRequirements(
    var requiresPower: Boolean = true,
    var requiresUnmetered: Boolean = true,
    var runImmediately: Boolean = false,
) {

    companion object {
        fun runImmediately() = NetworkRequirements(requiresPower = false, requiresUnmetered = false, runImmediately = true)
        fun needsUnmetered() = NetworkRequirements(requiresPower = false, requiresUnmetered = true, runImmediately = false)
        fun mostStringent() = NetworkRequirements(requiresPower = true, requiresUnmetered = true, runImmediately = false)
    }

    override fun toString(): String {
        val string = StringBuilder()
        val requires = ArrayList<String>()
        if (requiresPower) {
            requires.add("power")
        }
        if (requiresUnmetered) {
            requires.add("unmetered (WiFi)")
        }
        if (requires.isEmpty()) {
            string.append("No restrictions. ")
        } else {
            string.append("Requires ").append(TextUtils.join(" and ", requires)).append(". ")
        }
        string.append(if (runImmediately) "Run now." else "Run later.")
        return string.toString()
    }

    fun toWorkManagerEnum(): NetworkType {
        if (requiresUnmetered) {
            return NetworkType.UNMETERED
        }

        return NetworkType.CONNECTED
    }
}
