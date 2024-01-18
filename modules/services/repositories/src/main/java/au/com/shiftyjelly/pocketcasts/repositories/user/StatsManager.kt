package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import io.reactivex.Single

interface StatsManager {
    val timeSavedVariableSpeed: Long
    val timeSavedVariableSpeedSecs: Long
    val timeSavedSilenceRemoval: Long
    val timeSavedSilenceRemovalSecs: Long
    val timeSavedSkipping: Long
    val timeSavedSkippingSecs: Long
    val timeSavedSkippingIntro: Long
    val timeSavedSkippingIntroSecs: Long
    val totalTimeSaved: Long
    val totalListeningTime: Long
    val totalListeningTimeSecs: Long
    val statsStartTime: Long
    val statsStartTimeSecs: Long
    val isSynced: Boolean
    val isEmpty: Boolean
    val mergedTotalTimeSaved: Long
    val mergedTotalListeningTimeSec: Long

    val localStatsInServerFormat: Map<String, Long>
    var cachedMergedStats: Map<String, Long>

    fun addTimeSavedVariableSpeed(amountToAdd: Long)
    fun addTimeSavedSilenceRemoval(amountToAdd: Long)
    fun addTimeSavedSkipping(amountToAdd: Long)
    fun addTimeSavedAutoSkipping(amountToAdd: Long)
    fun addTotalListeningTime(amountToAdd: Long)

    fun initStatsEngine()
    fun persistTimes()
    fun reset()
    fun isSynced(settings: Settings): Boolean
    fun setSyncStatus(isSynced: Boolean)
    fun getServerStatsRx(): Single<StatsBundle>
    suspend fun getServerStats(): StatsBundle
    fun mergeStats(statsOne: Map<String, Long>?, statsTwo: Map<String, Long>?): Map<String, Long>
    suspend fun cacheMergedStats()

    interface OnStatsDownloadListener {
        fun downloaded(stats: Map<String, Long>)
        fun failed(errorMessage: String)
    }
}
