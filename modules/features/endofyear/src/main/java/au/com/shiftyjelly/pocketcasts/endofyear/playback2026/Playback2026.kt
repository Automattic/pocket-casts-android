package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Year
import kotlinx.coroutines.flow.StateFlow

object Playback2026 {
    val YEAR: Year = Year.of(2026)

    /** The single check every entry point (Podcasts sheet, Profile banner) and the story flow use. */
    val isAvailable: Boolean
        get() = FeatureFlag.isEnabled(Feature.PLAYBACK_2026)

    val isAvailableFlow: StateFlow<Boolean>
        get() = FeatureFlag.isEnabledFlow(Feature.PLAYBACK_2026)

    fun start(context: Context) {
        if (!isAvailable) return
        context.startActivity(Playback2026Activity.newIntent(context))
    }
}
