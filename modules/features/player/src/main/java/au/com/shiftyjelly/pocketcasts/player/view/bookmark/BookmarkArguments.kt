package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import kotlinx.parcelize.Parcelize

/**
 * Arguments for [BookmarkActivity] and [BookmarkFragment].
 */
@Parcelize
data class BookmarkArguments(
    val bookmarkUuid: String?,
    val episodeUuid: String,
    val timeSecs: Int,
    val podcastColors: PodcastColors,
    val isNewBookmark: Boolean = false,
    val fromEpisode: Boolean = false,
    val source: SourceView = SourceView.PLAYER,
) : Parcelable
