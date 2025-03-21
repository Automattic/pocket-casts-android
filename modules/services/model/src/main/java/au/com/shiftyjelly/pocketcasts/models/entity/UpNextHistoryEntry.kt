package au.com.shiftyjelly.pocketcasts.models.entity

import java.util.Date

data class UpNextHistoryEntry(
    val date: Date,
    val episodeCount: Int,
)
