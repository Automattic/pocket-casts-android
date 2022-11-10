package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

class StoryTopFivePodcasts(
    val topPodcasts: List<TopPodcast>,
) : Story()
