package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import kotlinx.parcelize.Parcelize

/**
 * Arguments for [BookmarkTranscriptEditActivity] and [BookmarkTranscriptEditFragment].
 */
@Parcelize
data class BookmarkTranscriptEditArguments(
    val bookmarkUuid: String,
    val episodeUuid: String,
    val podcastColors: PodcastColors,
) : Parcelable
