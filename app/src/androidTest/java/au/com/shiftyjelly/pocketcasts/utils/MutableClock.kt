package au.com.shiftyjelly.pocketcasts.utils

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration

class MutableClock(
    private var instant: Instant = Instant.EPOCH,
    private val zoneId: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun instant() = instant

    override fun getZone() = zoneId

    override fun withZone(zone: ZoneId) = MutableClock(instant, zone)

    operator fun plusAssign(duration: Duration) {
        instant = instant.plusMillis(duration.inWholeMilliseconds)
    }

    operator fun minusAssign(duration: Duration) {
        instant = instant.minusMillis(duration.inWholeMilliseconds)
    }
}
