package au.com.shiftyjelly.pocketcasts.settings.util

import android.content.res.Resources
import androidx.annotation.StringRes
import java.util.Random
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class FunnyTimeConverter {

    private val TIME_UNITS = arrayOf(
        FunnyTimeUnit(LR.string.settings_stats_funny_babies, 250.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_blinked, 7.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_lightning, 360.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_skin, 583.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_sneezed, 0.00694),
        FunnyTimeUnit(LR.string.settings_stats_funny_emails, 100000000.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_tweets, 121527.77),
        FunnyTimeUnit(LR.string.settings_stats_funny_air_biscuits, 0.0118),
        FunnyTimeUnit(LR.string.settings_stats_funny_laces, 6.0),
        FunnyTimeUnit(LR.string.settings_stats_funny_air_balloon, 0.000051994),
        FunnyTimeUnit(LR.string.settings_stats_funny_searches, 2276867.0)
    )

    fun timeSecsToFunnyText(timeSecs: Long, resources: Resources): String {
        // don't bother if the listening time is less than 1 minute
        if (timeSecs < 60) {
            return resources.getString(LR.string.settings_stats_funny_empty)
        }

        val rand = Random()
        while (true) {
            val randomIndex = rand.nextInt(TIME_UNITS.size)
            val unit = TIME_UNITS[randomIndex]
            if (unit.suitableFor(timeSecs)) return unit.funnyTextForSecs(timeSecs, resources)
        }
    }

    private inner class FunnyTimeUnit(@StringRes private val formatStringId: Int, private val timesPerMinute: Double) {

        fun funnyTextForSecs(timeSecs: Long, resources: Resources): String {
            val mins = (timeSecs / 60).toDouble()
            val amount = mins * timesPerMinute

            return resources.getString(formatStringId, amount)
        }

        fun suitableFor(timeSecs: Long): Boolean {
            val mins = (timeSecs / 60).toDouble()
            val amount = mins * timesPerMinute

            return amount >= 1
        }
    }
}
