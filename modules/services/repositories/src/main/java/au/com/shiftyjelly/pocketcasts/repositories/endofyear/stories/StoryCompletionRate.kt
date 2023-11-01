package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

class StoryCompletionRate(
    val percent: Float,
) : Story() {
    override val identifier: String = "completion_rate"
}
