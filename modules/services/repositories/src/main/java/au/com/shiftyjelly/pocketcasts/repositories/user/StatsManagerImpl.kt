package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class StatsManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings
) : StatsManager {

    companion object {
        private const val SYNC_STATUS = "sync_status"

        private const val LOCAL_KEY_VARIABLE_SPEED = "millis_saved_variable_speed"
        private const val LOCAL_KEY_SILENCE_REMOVAL = "millis_saved_silence_removal"
        private const val LOCAL_KEY_SKIPPING = "millis_skipped"
        private const val LOCAL_KEY_INTRO_SKIPPING = "millis_skipped_intro"
        private const val LOCAL_KEY_TOTAL_LISTENED = "millis_listened"

        private const val STATS_START_TIME = "millis_recording_started"

        private var millisSavedDynamicSpeed: Long = -1
        private var millisSavedVariableSpeed: Long = -1
        private var millisListenedTo: Long = -1
        private var millisSkipped: Long = -1
        private var millisSavedAutoSkipping: Long = -1

        private var changes = false
    }

    override val timeSavedVariableSpeed: Long
        get() {
            if (millisSavedVariableSpeed < 0) {
                millisSavedVariableSpeed = getTimeForKey(LOCAL_KEY_VARIABLE_SPEED)
            }
            return millisSavedVariableSpeed
        }

    override val timeSavedVariableSpeedSecs: Long
        get() = timeSavedVariableSpeed / 1000

    override val timeSavedSilenceRemoval: Long
        get() {
            if (millisSavedDynamicSpeed < 0) {
                millisSavedDynamicSpeed = getTimeForKey(LOCAL_KEY_SILENCE_REMOVAL)
            }
            return millisSavedDynamicSpeed
        }

    override val timeSavedSilenceRemovalSecs: Long
        get() = timeSavedSilenceRemoval / 1000

    override val timeSavedSkipping: Long
        get() {
            if (millisSkipped < 0) {
                millisSkipped = getTimeForKey(LOCAL_KEY_SKIPPING)
            }
            return millisSkipped
        }

    override val timeSavedSkippingSecs: Long
        get() = timeSavedSkipping / 1000

    override val timeSavedSkippingIntro: Long
        get() {
            if (millisSavedAutoSkipping < 0) {
                millisSavedAutoSkipping = getTimeForKey(LOCAL_KEY_INTRO_SKIPPING)
            }
            return millisSavedAutoSkipping
        }

    override val timeSavedSkippingIntroSecs: Long
        get() = timeSavedSkippingIntro / 1000

    override val totalTimeSaved: Long
        get() = timeSavedVariableSpeed + timeSavedSilenceRemoval + timeSavedSkipping + timeSavedSkippingIntro

    override val mergedTotalTimeSaved: Long
        get() = getCachedValue(StatsBundle.SERVER_KEY_AUTO_SKIPPING) + getCachedValue(StatsBundle.SERVER_KEY_SILENCE_REMOVAL) + getCachedValue(
            StatsBundle.SERVER_KEY_SKIPPING
        ) + getCachedValue(
            StatsBundle.SERVER_KEY_VARIABLE_SPEED
        )

    override val totalListeningTime: Long
        get() {
            if (millisListenedTo < 0) {
                millisListenedTo = getTimeForKey(LOCAL_KEY_TOTAL_LISTENED)
            }
            return millisListenedTo
        }

    override val mergedTotalListeningTimeSec: Long
        get() = cachedMergedStats[StatsBundle.SERVER_KEY_TOTAL_LISTENED] ?: 0L

    override val totalListeningTimeSecs: Long
        get() = totalListeningTime / 1000

    override val statsStartTime: Long
        get() = settings.getLongForKey(STATS_START_TIME, 0)

    override val statsStartTimeSecs: Long
        get() = statsStartTime / 1000

    override val isSynced: Boolean
        get() = settings.getBooleanForKey(SYNC_STATUS, true)

    override val isEmpty: Boolean
        get() = totalTimeSaved <= 0 && totalListeningTime <= 0

    override val localStatsInServerFormat: Map<String, Long>
        get() {
            val stats = HashMap<String, Long>()
            stats[StatsBundle.SERVER_KEY_VARIABLE_SPEED] = timeSavedVariableSpeedSecs
            stats[StatsBundle.SERVER_KEY_SILENCE_REMOVAL] = timeSavedSilenceRemovalSecs
            stats[StatsBundle.SERVER_KEY_SKIPPING] = timeSavedSkippingSecs
            stats[StatsBundle.SERVER_KEY_AUTO_SKIPPING] = timeSavedSkippingIntroSecs
            stats[StatsBundle.SERVER_KEY_TOTAL_LISTENED] = totalListeningTimeSecs
            stats[StatsBundle.SERVER_KEY_STARTED_AT] = statsStartTimeSecs
            return stats
        }

    override var cachedMergedStats: Map<String, Long> = localStatsInServerFormat
    private var cachedServerStats: Map<String, Long> = mapOf()

    override fun addTimeSavedVariableSpeed(amountToAdd: Long) {
        millisSavedVariableSpeed = timeSavedVariableSpeed + amountToAdd
        changes = true
    }

    override fun addTimeSavedSilenceRemoval(amountToAdd: Long) {
        millisSavedDynamicSpeed = timeSavedSilenceRemoval + amountToAdd
        changes = true
    }

    override fun addTimeSavedSkipping(amountToAdd: Long) {
        millisSkipped = timeSavedSkipping + amountToAdd
        changes = true
    }

    override fun addTimeSavedAutoSkipping(amountToAdd: Long) {
        millisSavedAutoSkipping = timeSavedSkippingIntro + amountToAdd
        changes = true
    }

    override fun addTotalListeningTime(amountToAdd: Long) {
        millisListenedTo = totalListeningTime + amountToAdd
        changes = true
    }

    override fun initStatsEngine() {
        if (statsStartTime == 0L) {
            settings.setLongForKey(STATS_START_TIME, System.currentTimeMillis())
        }

        val settingsServerStats = mapOf(
            StatsBundle.SERVER_KEY_AUTO_SKIPPING to getTimeForKey(StatsBundle.SERVER_KEY_AUTO_SKIPPING),
            StatsBundle.SERVER_KEY_SILENCE_REMOVAL to getTimeForKey(StatsBundle.SERVER_KEY_SILENCE_REMOVAL),
            StatsBundle.SERVER_KEY_SKIPPING to getTimeForKey(StatsBundle.SERVER_KEY_SKIPPING),
            StatsBundle.SERVER_KEY_STARTED_AT to getTimeForKey(StatsBundle.SERVER_KEY_STARTED_AT),
            StatsBundle.SERVER_KEY_TOTAL_LISTENED to getTimeForKey(StatsBundle.SERVER_KEY_TOTAL_LISTENED),
            StatsBundle.SERVER_KEY_VARIABLE_SPEED to getTimeForKey(StatsBundle.SERVER_KEY_VARIABLE_SPEED)
        )

        cachedServerStats = settingsServerStats
        cachedMergedStats = mergeStats(localStatsInServerFormat, settingsServerStats)
    }

    /**
     * To conserve battery we want to keep these stats in memory. When it makes sense to, call this
     * method to actually save them between app launches
     */
    override fun persistTimes() {
        saveTimeForKey(LOCAL_KEY_VARIABLE_SPEED, millisSavedVariableSpeed)
        saveTimeForKey(LOCAL_KEY_SILENCE_REMOVAL, millisSavedDynamicSpeed)
        saveTimeForKey(LOCAL_KEY_SKIPPING, millisSkipped)
        saveTimeForKey(LOCAL_KEY_INTRO_SKIPPING, millisSavedAutoSkipping)
        saveTimeForKey(LOCAL_KEY_TOTAL_LISTENED, millisListenedTo)

        cachedServerStats.forEach { (key, value) ->
            saveTimeForKey(key, value)
        }

        setSyncStatus(!changes)
    }

    override fun reset() {
        millisSavedDynamicSpeed = 0
        millisSavedVariableSpeed = 0
        millisListenedTo = 0
        millisSkipped = 0
        millisSavedAutoSkipping = 0
        changes = false
        cachedServerStats = mapOf()
        cachedMergedStats = mapOf()
        persistTimes()
    }

    override fun isSynced(settings: Settings): Boolean {
        return settings.getBooleanForKey(SYNC_STATUS, true)
    }

    override fun setSyncStatus(isSynced: Boolean) {
        settings.setBooleanForKey(SYNC_STATUS, isSynced)
    }

    override fun getServerStatsRx(): Single<StatsBundle> {
        return rxSingle(Dispatchers.IO) { getServerStats() }
    }

    override suspend fun getServerStats(): StatsBundle {
        return syncManager.loadStats()
    }

    override suspend fun cacheMergedStats() {
        try {
            val stats = getServerStats()
            cachedServerStats = stats.values
            cachedMergedStats = mergeStats(stats.values, localStatsInServerFormat)
            persistTimes()
        } catch (ex: Exception) {
            LogBuffer.logException(LogBuffer.TAG_BACKGROUND_TASKS, ex, "Could not load server stats for cache")
        }
    }

    override fun mergeStats(statsOne: Map<String, Long>?, statsTwo: Map<String, Long>?): Map<String, Long> {
        if (statsOne == null) {
            return statsTwo ?: HashMap()
        }
        if (statsTwo == null) {
            return statsOne
        }
        val stats = HashMap<String, Long>()
        val itr = statsOne.keys.iterator()
        while (itr.hasNext()) {
            val key = itr.next()
            val valueOne = statsOne[key]
            val valueTwo = statsTwo[key]

            // choose the oldest started at
            if (key == StatsBundle.SERVER_KEY_STARTED_AT) {
                val value = when {
                    valueOne == null -> valueTwo
                    valueTwo == null -> valueOne
                    else -> if (valueOne > valueTwo) valueTwo else valueOne
                }
                stats[StatsBundle.SERVER_KEY_STARTED_AT] = value ?: 0
                continue
            }

            val valueOneLong = valueOne ?: 0
            val valueTwoLong = valueTwo ?: 0
            stats[key] = valueOneLong + valueTwoLong
        }

        return stats
    }

    private fun getTimeForKey(key: String): Long {
        return settings.getLongForKey(key, 0)
    }

    private fun saveTimeForKey(key: String, value: Long) {
        if (value < 0) return
        settings.setLongForKey(key, value)
    }

    private fun getCachedValue(key: String): Long {
        return cachedMergedStats[key] ?: 0L
    }
}
