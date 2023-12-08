package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted

class StoryCompletionRate(
    val episodesStartedAndCompleted: EpisodesStartedAndCompleted,
) : Story() {
    override val identifier: String = "completion_rate"
    override val plusOnly: Boolean = true
}
