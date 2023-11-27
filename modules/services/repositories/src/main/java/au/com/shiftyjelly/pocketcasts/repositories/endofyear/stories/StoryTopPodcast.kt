package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

class StoryTopPodcast(
    val topPodcasts: List<TopPodcast>,
) : Story() {
    override val identifier: String = "top_one_podcast"
    val topPodcast = topPodcasts[0]
}
