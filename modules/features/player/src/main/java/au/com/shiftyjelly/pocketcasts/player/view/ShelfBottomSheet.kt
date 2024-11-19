package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.player.view.shelf.ShelfBottomSheetPage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.getValue

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    private val episodeId: String
        get() = requireNotNull(arguments?.getString(ARG_EPISODE_ID))

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
    ) = content {
        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }
        AppTheme(theme.activeTheme) {
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
