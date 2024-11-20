package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarkFragment : Fragment() {

    private val viewModel: BookmarkViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        LaunchedEffect(Unit) { viewModel.load(BookmarkArguments.createFromArguments(arguments)) }
        AppThemeWithBackground(Theme.ThemeType.DARK) {
            val uiState: BookmarkViewModel.UiState by viewModel.uiState.collectAsState()
            BookmarkPage(
                isNewBookmark = uiState.isNewBookmark,
                title = uiState.title,
                tintColor = uiState.tintColor,
                backgroundColor = uiState.backgroundColor,
                onTitleChange = { viewModel.changeTitle(it) },
                onSave = ::saveBookmark,
                onClose = ::close,
                modifier = Modifier.background(uiState.backgroundColor),
            )
        }
    }

    private fun saveBookmark() {
        viewModel.saveBookmark(onSaved = { bookmark, isExisting ->
            bookmarkSaved(bookmark, isExisting)
        })
    }

    private fun bookmarkSaved(bookmark: Bookmark, isExistingBookmark: Boolean) {
        val intent = BookmarkActivityContract.createIntent(
            bookmarkUuid = bookmark.uuid,
            title = bookmark.title,
            tintColor = viewModel.uiState.value.tintColor,
            isExistingBookmark = isExistingBookmark,
        )
        requireActivity().run {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun close() {
        requireActivity().run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
