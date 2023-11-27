package au.com.shiftyjelly.pocketcasts.endofyear.utils

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import kotlin.math.min

fun List<TopPodcast>.atSafeIndex(
    index: Int,
    maxSize: Int = 8
): Podcast {
    val size = min(size, maxSize)
    return this[(index % size).coerceAtMost(size - 1)]
        .toPodcast()
}
