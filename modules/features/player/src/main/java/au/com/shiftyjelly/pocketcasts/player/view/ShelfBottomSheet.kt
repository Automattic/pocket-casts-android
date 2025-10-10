package au.com.shiftyjelly.pocketcasts.player.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.player.view.shelf.ShelfBottomSheetPage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireString
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    private val episodeId: String
        get() = requireArguments().requireString(ARG_EPISODE_ID)

    override val statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val shelfSharedViewModel: ShelfSharedViewModel by activityViewModels()
    private val shelfViewModel: ShelfViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShelfViewModel.Factory> { factory ->
                factory.create(
                    episodeId = episodeId,
                    isEditable = false,
                )
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        // set the initial background color to black to stop the dialog from flashing white
        setDialogTint(Color.BLACK)
        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            setDialogTint(backgroundColor)
        }
        AppTheme(Theme.ThemeType.DARK) {
            ShelfBottomSheetPage(
                shelfViewModel = shelfViewModel,
                shelfSharedViewModel = shelfSharedViewModel,
                playerViewModel = playerViewModel,
                onEditButtonClick = {
                    (activity as FragmentHostListener).showModal(ShelfFragment.newInstance(episodeId))
                    dismiss()
                },
                onDismiss = { dismiss() },
            )
        }
    }

    companion object {
        private const val ARG_EPISODE_ID = "episode_id"
        fun newInstance(
            episodeId: String,
        ) = ShelfBottomSheet().apply {
            arguments = bundleOf(
                ARG_EPISODE_ID to episodeId,
            )
        }
    }
}
