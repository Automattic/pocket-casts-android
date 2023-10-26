package au.com.shiftyjelly.pocketcasts.endofyear.utils

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast

fun List<TopPodcast>.atSafeIndex(index: Int) =
    this[index.coerceAtMost(size - 1)]
        .toPodcast()
