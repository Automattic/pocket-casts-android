package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier

class StoryCompletionRate(
    val userTier: UserTier = UserTier.Plus,
) : Story() {
    override val identifier: String = "completion_rate"
}
