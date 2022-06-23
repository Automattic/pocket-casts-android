package au.com.shiftyjelly.pocketcasts.models.db.helper

// A holder of podcast substitutes when needed for UserEpisodes
// I thought about making this an actual Podcast but then there
// is nothing stopping it getting passed in to a podcast manager function.
object UserEpisodePodcastSubstitute {
    const val uuid = "da7aba5e-f11e-f11e-f11e-da7aba5ef11e"
    const val title = "Custom Episode"
}
