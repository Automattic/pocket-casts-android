package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarkFragment : BaseFragment() {
    companion object {
        private const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(args: BookmarkArguments) = BookmarkFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to args)
        }
    }

    private val viewModel: BookmarkViewModel by viewModels()

    private val args get() = requireArguments().requireParcelable<BookmarkArguments>(NEW_INSTANCE_KEY)

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        LaunchedEffect(Unit) { viewModel.load(args) }

        AppThemeWithBackground(theme.activeTheme) {
            val uiState: BookmarkViewModel.UiState by viewModel.uiState.collectAsState()

            CallOnce {
                viewModel.onShown()
            }

            CompositionLocalProvider(
                LocalPodcastColors provides args.podcastColors,
            ) {
                val playerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
                BookmarkPage(
                    isNewBookmark = uiState.isNewBookmark,
                    title = uiState.title,
                    playerColors = playerColors,
                    onTitleChange = { viewModel.changeTitle(it) },
                    onSave = ::saveBookmark,
                    onClose = ::close,
                    modifier = Modifier
                        .background(playerColors.background01)
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.systemBars)),
                )
            }
        }
    }

    private fun saveBookmark() {
        viewModel.onSubmitBookmark()
        viewModel.saveBookmark(onSaved = { bookmark, isExisting ->
            bookmarkSaved(bookmark, isExisting)
        })
    }

    private fun bookmarkSaved(bookmark: Bookmark, isExistingBookmark: Boolean) {
        val intent = BookmarkActivityContract.createIntent(
            bookmarkUuid = bookmark.uuid,
            title = bookmark.title,
            tintColor = args.podcastColors.playerTint,
            isExistingBookmark = isExistingBookmark,
        )
        requireActivity().run {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun close() {
        viewModel.onClose()
        requireActivity().run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
