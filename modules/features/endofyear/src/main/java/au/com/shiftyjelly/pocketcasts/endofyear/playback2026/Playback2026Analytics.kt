package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import com.automattic.eventhorizon.Trackable
import com.automattic.pocketcasts.playback.PlaybackAnalyticsEvent
import java.time.Year
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * An `end_of_year` event from inside the Playback 2026 stories, as the app tracks it: the event
 * Playback 2025 sent, with the same name and story values so 2026 compares with 2025, and
 * `current_year` (PCIOS-1262, POC-1006).
 *
 * The 2026 events and values (`end_of_year_quiz_revealed`, `end_of_year_archetype_assigned`,
 * the new stories, `index`, `count`, `revisit`, `variant`, `finished`) are added to the schema by
 * Automattic/EventHorizonSchemas#158, which the generated types in this app predate. Until it
 * moves to a build that has them, this sends each event under its schema name, with the
 * properties typed as the generated type will have them: ints as [Long], booleans as [Boolean],
 * the rest as strings.
 */
@Parcelize
internal data class Playback2026Event(
    override val analyticsName: String,
    override val analyticsProperties: Map<String, @RawValue Any>,
) : Trackable

internal fun PlaybackAnalyticsEvent.toTrackable(year: Year = Playback2026.YEAR): Trackable = Playback2026Event(
    analyticsName = name,
    analyticsProperties = properties.mapValues { (key, value) -> typed(key, value) } + ("current_year" to year.value.toLong()),
)

private fun typed(key: String, value: String): Any = when (key) {
    "index", "count" -> value.toLongOrNull() ?: value
    "revisit", "finished" -> value.toBooleanStrictOrNull() ?: value
    else -> value
}
