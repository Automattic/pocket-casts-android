package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.runtime.Composable
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackgroundStyle
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlin.math.min

fun List<TopPodcast>.atSafeIndex(
    index: Int,
    maxSize: Int = 8
): Podcast {
    val size = min(size, maxSize)
    return this[(index % size).coerceAtMost(size - 1)]
        .toPodcast()
}

@Composable
fun blurredBackgroundStyle(userTier: UserTier) = when (userTier) {
    UserTier.Patron, UserTier.Plus -> StoryBlurredBackgroundStyle.Plus
    UserTier.Free -> StoryBlurredBackgroundStyle.Default
}
