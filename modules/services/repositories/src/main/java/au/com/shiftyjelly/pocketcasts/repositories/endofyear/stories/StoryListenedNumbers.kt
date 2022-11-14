package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

class StoryListenedNumbers(
    val listenedNumbers: ListenedNumbers,
    val topPodcasts: List<TopPodcast>,
) : Story()
