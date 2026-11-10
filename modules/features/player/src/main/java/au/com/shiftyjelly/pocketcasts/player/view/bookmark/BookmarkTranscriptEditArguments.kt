package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import kotlinx.parcelize.Parcelize

/**
 * Arguments for [BookmarkTranscriptEditActivity] and [BookmarkTranscriptEditFragment].
 */
@Parcelize
data class BookmarkTranscriptEditArguments(
    val episodeUuid: String,
    val podcastUuid: String?,
    val passage: String?,
    val passageLocation: Int?,
    val referenceTime: Int?,
    val podcastColors: PodcastColors,
) : Parcelable
