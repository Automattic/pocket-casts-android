package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime

class StoryYearOverYear(
    val yearOverYearListeningTime: YearOverYearListeningTime,
) : Story() {
    override val identifier: String = "year_over_year"
    override val plusOnly: Boolean = true
}
