package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarkTranscriptEditFragment : BaseFragment() {
    companion object {
        private const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(args: BookmarkTranscriptEditArguments) = BookmarkTranscriptEditFragment().apply {
            arguments = Bundle().apply {
                putParcelable(NEW_INSTANCE_KEY, args)
            }
        }
    }

    private val viewModel: BookmarkTranscriptEditViewModel by viewModels()

    private val args get() = requireArguments().requireParcelable<BookmarkTranscriptEditArguments>(NEW_INSTANCE_KEY)

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        LaunchedEffect(Unit) { viewModel.load(args) }

        AppThemeWithBackground(theme.activeTheme) {
            val uiState by viewModel.uiState.collectAsState()

            CompositionLocalProvider(
                LocalPodcastColors provides args.podcastColors,
            ) {
                val playerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
                BookmarkTranscriptEditPage(
                    uiState = uiState,
                    playerColors = playerColors,
                    onPassageChange = viewModel::onPassageChange,
                    onSave = ::save,
                    onClose = ::close,
                    modifier = Modifier
                        .background(playerColors.background01)
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }

    private fun save() {
        viewModel.save(
            onSaved = {
                requireActivity().run {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            },
        )
    }

    private fun close() {
        requireActivity().run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
