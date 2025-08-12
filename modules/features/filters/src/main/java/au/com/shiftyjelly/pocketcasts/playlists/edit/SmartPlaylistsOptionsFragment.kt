package au.com.shiftyjelly.pocketcasts.playlists.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.SmartPlaylistViewModel
import au.com.shiftyjelly.pocketcasts.playlists.SmartPlaylistViewModel.Companion.DOWNLOAD_ALL_LIMIT
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class SmartPlaylistsOptionsFragment : BaseDialogFragment() {
    private val viewModel by viewModels<SmartPlaylistViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        var isSelectingSortType by rememberSaveable { mutableStateOf(false) }

        val uiState by viewModel.uiState.collectAsState()
        val playlist = uiState.smartPlaylist

        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                        .size(56.dp, 4.dp),
                )
                if (playlist != null) {
                    AnimatedContent(
                        targetState = isSelectingSortType,
                    ) { isSorting ->
                        if (isSorting) {
                            SmartPlaylistSortOptionsColumn(
                                selectedSortType = playlist.episodeSortType,
                                onSelectSortType = { type ->
                                    viewModel.updateSortType(type)
                                    dismiss()
                                },
                            )
                        } else {
                            SmartPlaylistOptionsColumn(
                                sortType = playlist.episodeSortType,
                                hasEpisodes = playlist.totalEpisodeCount > 0,
                                onClickSelectAll = {
                                    viewModel.startMultiSelecting()
                                    dismiss()
                                },
                                onClickSortBy = { isSelectingSortType = true },
                                onClickDownloadAll = {
                                    downloadAll(playlist.totalEpisodeCount)
                                    dismiss()
                                },
                                onClickChromecast = {
                                    viewModel.startChromeCast()
                                    dismiss()
                                },
                                onClickOpenSettings = {
                                    viewModel.showSettings()
                                    dismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun downloadAll(episodeCount: Int) {
        when {
            parentFragmentManager.findFragmentByTag("download_confirm") != null -> Unit

            episodeCount < 5 -> {
                viewModel.downloadAll()
            }

            episodeCount in 5..DOWNLOAD_ALL_LIMIT -> {
                val dialog = ConfirmationDialog()
                    .setButtonType(ButtonType.Normal(getString(LR.string.download_warning_button, episodeCount)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(getString(LR.string.download_warning_title))
                    .setOnConfirm { viewModel.downloadAll() }
                dialog.show(parentFragmentManager, "download_confirm")
            }

            else -> {
                val dialog = ConfirmationDialog()
                    .setButtonType(ButtonType.Normal(getString(LR.string.download_warning_button, DOWNLOAD_ALL_LIMIT)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(getString(LR.string.download_warning_title))
                    .setSummary(getString(LR.string.download_warning_limit_summary, DOWNLOAD_ALL_LIMIT))
                    .setOnConfirm { viewModel.downloadAll() }
                dialog.show(parentFragmentManager, "download_confirm")
            }
        }
    }
}
