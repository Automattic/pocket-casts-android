package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EndOfYearStoryOpenSourceType
import java.time.Year

object Playback2026 {
    val YEAR: Year = Year.of(2026)

    /** Where the stories were opened from: `end_of_year_stories_shown`'s `source`. */
    enum class Source(val analyticsValue: EndOfYearStoryOpenSourceType) {
        Modal(EndOfYearStoryOpenSourceType.Modal),
        Profile(EndOfYearStoryOpenSourceType.Profile),
    }

    fun start(context: Context, source: Source) {
        if (!FeatureFlag.isEnabled(Feature.PLAYBACK_2026)) return
        context.startActivity(Playback2026Activity.newIntent(context, source))
    }
}
