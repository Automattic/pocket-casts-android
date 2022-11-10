package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

class StoryListeningTime(
    val listeningTimeInSecs: Long,
    val podcasts: List<TopPodcast>,
) : Story()
