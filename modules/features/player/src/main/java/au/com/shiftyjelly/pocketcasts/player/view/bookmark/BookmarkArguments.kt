package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf

/**
 * Arguments for [BookmarkActivity] and [BookmarkFragment].
 */
class BookmarkArguments(
    val bookmarkUuid: String? = null,
    val episodeUuid: String,
    val timeSecs: Int,
    val backgroundColor: Int,
    val tintColor: Int,
) {

    companion object {
        private const val ARGUMENT_BOOKMARK_UUID = "BOOKMARK_UUID"
        private const val ARGUMENT_EPISODE_UUID = "EPISODE_UUID"
        private const val ARGUMENT_TIME = "TIME"
        private const val ARGUMENT_BACKGROUND_COLOR = "BACKGROUND_COLOR"
        private const val ARGUMENT_TINT_COLOR = "TINT_COLOR"

        fun createFromIntent(intent: Intent): BookmarkArguments {
            return BookmarkArguments(
                bookmarkUuid = intent.getStringExtra(ARGUMENT_BOOKMARK_UUID),
                episodeUuid = intent.getStringExtra(ARGUMENT_EPISODE_UUID) ?: throw IllegalArgumentException("Episode UUID not set"),
                timeSecs = intent.getIntExtra(ARGUMENT_TIME, 0),
                backgroundColor = intent.getIntExtra(ARGUMENT_BACKGROUND_COLOR, 0),
                tintColor = intent.getIntExtra(ARGUMENT_TINT_COLOR, 0)
            )
        }

        fun createFromArguments(arguments: Bundle?): BookmarkArguments {
            return BookmarkArguments(
                bookmarkUuid = arguments?.getString(ARGUMENT_BOOKMARK_UUID),
                episodeUuid = arguments?.getString(ARGUMENT_EPISODE_UUID) ?: throw IllegalArgumentException("Episode UUID not set"),
                timeSecs = arguments.getInt(ARGUMENT_TIME),
                backgroundColor = arguments.getInt(ARGUMENT_BACKGROUND_COLOR),
                tintColor = arguments.getInt(ARGUMENT_TINT_COLOR)
            )
        }
    }

    fun getIntent(context: Context): Intent {
        return Intent(context, BookmarkActivity::class.java).apply {
            putExtra(ARGUMENT_BOOKMARK_UUID, bookmarkUuid)
            putExtra(ARGUMENT_EPISODE_UUID, episodeUuid)
            putExtra(ARGUMENT_TIME, timeSecs)
            putExtra(ARGUMENT_BACKGROUND_COLOR, backgroundColor)
            putExtra(ARGUMENT_TINT_COLOR, tintColor)
        }
    }

    fun buildFragment(): BookmarkFragment {
        return BookmarkFragment().apply {
            arguments = bundleOf(
                ARGUMENT_BOOKMARK_UUID to bookmarkUuid,
                ARGUMENT_EPISODE_UUID to episodeUuid,
                ARGUMENT_TIME to timeSecs,
                ARGUMENT_BACKGROUND_COLOR to backgroundColor,
                ARGUMENT_TINT_COLOR to tintColor
            )
        }
    }
}
