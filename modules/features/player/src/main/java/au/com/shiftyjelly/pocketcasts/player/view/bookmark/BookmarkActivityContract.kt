package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract.BookmarkResult

/**
 * Handles returning a result from the [BookmarkActivity] to the caller.
 * The result is a [BookmarkResult] which contains the created bookmark's UUID and title.
 * Also the tint color so the caller can easily show a Snackbar with the correct action color.
 */
class BookmarkActivityContract : ActivityResultContract<Intent, BookmarkActivityContract.BookmarkResult?>() {

    companion object {
        const val RESULT_BOOKMARK_UUID = "RESULT_BOOKMARK_UUID"
        const val RESULT_TITLE = "RESULT_TITLE"
        const val RESULT_TINT_COLOR = "RESULT_TINT_COLOR"
        const val RESULT_EXISTING_BOOKMARK = "RESULT_EXISTING_BOOKMARK"

        fun createIntent(bookmarkUuid: String, title: String, tintColor: Color, isExistingBookmark: Boolean): Intent {
            return Intent().apply {
                putExtra(RESULT_BOOKMARK_UUID, bookmarkUuid)
                putExtra(RESULT_TITLE, title)
                putExtra(RESULT_TINT_COLOR, tintColor.toArgb())
                putExtra(RESULT_EXISTING_BOOKMARK, isExistingBookmark)
            }
        }
    }

    data class BookmarkResult(
        val bookmarkUuid: String? = null,
        val title: String,
        val tintColor: Int,
        val isExistingBookmark: Boolean
    )

    override fun createIntent(context: Context, input: Intent): Intent = input

    override fun parseResult(resultCode: Int, intent: Intent?): BookmarkResult? {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            val bookmarkUuid = intent.getStringExtra(RESULT_BOOKMARK_UUID)
            val title = intent.getStringExtra(RESULT_TITLE)
            val tintColor = intent.getIntExtra(RESULT_TINT_COLOR, Color.White.toArgb())
            val existingBookmark = intent.getBooleanExtra(RESULT_EXISTING_BOOKMARK, false)
            if (bookmarkUuid != null) {
                return BookmarkResult(bookmarkUuid = bookmarkUuid, title = title ?: "", tintColor = tintColor, isExistingBookmark = existingBookmark)
            }
        }
        return null
    }
}
