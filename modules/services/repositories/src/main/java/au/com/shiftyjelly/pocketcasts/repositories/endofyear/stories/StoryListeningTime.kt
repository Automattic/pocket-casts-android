package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

class StoryListeningTime(
    val listeningTimeInSecs: Long,
) : Story() {
    override val identifier: String = "listening_time"
}
