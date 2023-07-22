package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
    ): View {
        viewModel.load(BookmarkArguments.createFromArguments(arguments))
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(Theme.ThemeType.DARK) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                    val uiState: BookmarkViewModel.UiState by viewModel.uiState.collectAsState()
                    BookmarkPage(
                        isNewBookmark = uiState.isNewBookmark,
                        title = uiState.title,
                        tintColor = uiState.tintColor,
                        backgroundColor = uiState.backgroundColor,
                        onTitleChange = { viewModel.changeTitle(it) },
                        onSave = ::saveBookmark,
                        onClose = ::close,
                        modifier = Modifier.background(uiState.backgroundColor)
                    )
                }
            }
        }
    }

    private fun saveBookmark() {
        viewModel.saveBookmark(onSaved = { bookmark -> bookmarkSaved(bookmark) })
    }

    private fun bookmarkSaved(bookmark: Bookmark) {
        val intent = BookmarkActivityContract.createIntent(
            bookmarkUuid = bookmark.uuid,
            title = bookmark.title,
            tintColor = viewModel.uiState.value.tintColor
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
