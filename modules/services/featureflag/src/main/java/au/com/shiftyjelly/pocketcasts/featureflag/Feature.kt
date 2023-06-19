package au.com.shiftyjelly.pocketcasts.featureflag

enum class Feature(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
) {
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
