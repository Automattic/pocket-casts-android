package au.com.shiftyjelly.pocketcasts.featureflag

interface Feature {
    val key: String
    val title: String
    val defaultValue: Boolean
}

enum class FeatureFlag(
    override val key: String,
    override val title: String,
    override val defaultValue: Boolean,
) : Feature {
    END_OF_YEAR_ENABLED(
        key = "end_of_year_enabled",
        title = "End of Year",
        defaultValue = false
    ),
    SHOW_RATINGS_ENABLED(
        key = "show_ratings_enabled",
        title = "Show Ratings",
        defaultValue = false
    ),
    ADD_PATRON_ENABLED(
        key = "add_patron_enabled",
        title = "Patron",
        defaultValue = false
    ),
}
