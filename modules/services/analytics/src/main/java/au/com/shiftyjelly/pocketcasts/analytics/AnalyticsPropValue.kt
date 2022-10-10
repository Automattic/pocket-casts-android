package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings

/**
 * This class exists to ensure that only types which can be used by Tracks are passed
 * as event properties. For that reason, the primary constructor accepting Any types is
 * private.
 */
data class AnalyticsPropValue private constructor(val propValue: Any) {
    constructor(s: String) : this(s as Any)
    constructor(bool: Boolean) : this(bool as Any)
    constructor(num: Int) : this(num as Any)
    constructor(num: Long) : this(num as Any)
}

// Can't add these directly to the classes because it would create a circular dependency in the
// modules, so adding extension fields here for convenience
val PodcastsSortType.analyticsValue
    get() = AnalyticsPropValue(analyticsString)
val Settings.BadgeType.analyticsValue
    get() = AnalyticsPropValue(analyticsString)
val Settings.PodcastGridLayoutType.analyticsValue
    get() = AnalyticsPropValue(analyticsString)
