package au.com.shiftyjelly.pocketcasts.chat

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatScreen
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ChatFragment : BaseDialogFragment() {
    companion object {
        private const val ARGS_KEY = "chat_args"

        fun newInstance(
            episodeUuid: String,
            podcastUuid: String?,
            episodeTitle: String,
            episodeSubtitle: String,
        ) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_KEY, Args(episodeUuid, podcastUuid, episodeTitle, episodeSubtitle))
            }
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<ChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setEpisodeInfo(
            episodeUuid = args.episodeUuid,
            episodeTitle = args.episodeTitle,
            episodeSubtitle = args.episodeSubtitle,
            podcastUuid = args.podcastUuid,
            welcomeMessage = getString(LR.string.chat_preview_ai_1),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val uiState by viewModel.uiState.collectAsState()

        AppTheme(theme.activeTheme) {
            val backgroundColor = MaterialTheme.theme.colors.primaryUi01
            LaunchedEffect(backgroundColor) {
                setDialogTint(backgroundColor.toArgb())
            }

            ChatScreen(
                uiState = uiState,
                onClickClose = { dismiss() },
                onClickMore = ::showOptionsDialog,
                onInputTextChange = viewModel::onInputTextChange,
                onSend = viewModel::onSend,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun showOptionsDialog() {
        OptionsDialog()
            .addTextOption(
                titleId = LR.string.chat_clear,
                imageId = IR.drawable.ic_delete,
                click = { viewModel.clearChat() },
            )
            .show(parentFragmentManager, "chat_options")
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bottomSheetView()?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.isDraggable = false
            behavior.maxHeight = resources.displayMetrics.heightPixels
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String?,
        val episodeTitle: String,
        val episodeSubtitle: String,
    ) : Parcelable
}
