package au.com.shiftyjelly.pocketcasts.repositories.download

import android.app.job.JobInfo
import android.text.TextUtils
import androidx.work.NetworkType

data class NetworkRequirements(
    var requiresPower: Boolean = true,
    var requiresUnmetered: Boolean = true,
    var runImmediately: Boolean = false
) {

    companion object {

        fun runImmediately() = NetworkRequirements(requiresPower = false, requiresUnmetered = false, runImmediately = true)
        fun anyNetwork() = NetworkRequirements(requiresPower = false, requiresUnmetered = false, runImmediately = false)
        fun needsUnmetered() = NetworkRequirements(requiresPower = false, requiresUnmetered = true, runImmediately = false)
        fun mostStringent() = NetworkRequirements(requiresPower = true, requiresUnmetered = true, runImmediately = false)

        fun fromJobId(jobId: Int): NetworkRequirements {
            for (requirements in all()) {
                if (requirements.jobId() == jobId) return requirements
            }
            return needsUnmetered()
        }

        private fun all(): List<NetworkRequirements> {
            val allPossible = ArrayList<NetworkRequirements>()

            allPossible.add(NetworkRequirements(requiresPower = false, requiresUnmetered = false))
            allPossible.add(NetworkRequirements(requiresPower = true, requiresUnmetered = true))

            allPossible.add(NetworkRequirements(requiresPower = false, requiresUnmetered = true))
            allPossible.add(NetworkRequirements(requiresPower = true, requiresUnmetered = false))

            allPossible.add(runImmediately())

            return allPossible
        }
    }

    fun downgradeIfRequired(requiresUnmetered: Boolean, requiresPower: Boolean) {
        if (this.requiresPower && !requiresPower) {
            this.requiresPower = false
        }
        if (this.requiresUnmetered && !requiresUnmetered) {
            this.requiresUnmetered = false
        }
    }

    fun requiredNetworkTypeAsJobInfoConstant(): Int {
        return if (requiresUnmetered) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY
    }

    fun jobId(): Int {
        return hashCode()
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
