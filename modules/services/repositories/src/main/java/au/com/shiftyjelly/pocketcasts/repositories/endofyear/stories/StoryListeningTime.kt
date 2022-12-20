package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

class StoryListeningTime(
    val listeningTimeInSecs: Long,
    val podcasts: List<TopPodcast>,
) : Story() {
    override val identifier: String = "listening_time"
}
