package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory

class StoryListenedCategories(
    val listenedCategories: List<ListenedCategory>
) : Story()
