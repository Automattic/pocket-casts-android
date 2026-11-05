package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Year

object Playback2026 {
    val YEAR: Year = Year.of(2026)

    fun start(context: Context) {
        if (!FeatureFlag.isEnabled(Feature.PLAYBACK_2026)) return
        context.startActivity(Playback2026Activity.newIntent(context))
    }
}
