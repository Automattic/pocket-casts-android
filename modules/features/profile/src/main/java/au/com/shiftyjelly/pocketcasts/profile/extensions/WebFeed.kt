package au.com.shiftyjelly.pocketcasts.profile.extensions

import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed

val WebFeed.displayHref: String
    get() = href
        .removePrefix("https://")
        .removePrefix("http://")
