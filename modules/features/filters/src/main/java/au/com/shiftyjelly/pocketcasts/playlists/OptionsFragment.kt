package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistViewModel.Companion.DOWNLOAD_ALL_LIMIT
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistOption
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistOptionsColumn
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistSortOptionsColumn
import au.com.shiftyjelly.pocketcasts.playlists.component.displayLabel
import au.com.shiftyjelly.pocketcasts.playlists.manual.EditPlaylistFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class OptionsFragment : BaseDialogFragment() {
    private val viewModel by viewModels<PlaylistViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState by viewModel.uiState.collectAsState()
        val playlist = uiState.playlist

        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                        .size(56.dp, 4.dp),
                )
                if (playlist != null) {
                    var isSelectingSortType by rememberSaveable { mutableStateOf(false) }

                    AnimatedContent(targetState = isSelectingSortType) { isSorting ->
                        if (isSorting) {
                            PlaylistSortOptionsColumn(
                                availableSortTypes = playlist.availableSortOptions,
                                selectedSortType = playlist.settings.sortType,
                                onSelectSortType = { type ->
                                    viewModel.updateSortType(type)
                                    dismiss()
                                },
                            )
                        } else {
                            PlaylistOptionsColumn(
                                options = playlistOptions(
                                    playlist = playlist,
                                    onClickChangeSortType = { isSelectingSortType = true },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun playlistOptions(
        playlist: Playlist,
        onClickChangeSortType: () -> Unit,
    ): List<PlaylistOption> {
        val playlistType = playlist.type
        val sortType = playlist.settings.sortType
        val sortTypeLabel = sortType.displayLabel()

        val episodeCount = playlist.metadata.displayedAvailableEpisodeCount
        val archivedEpisodeCount = playlist.metadata.archivedEpisodeCount
        val isShowingArchived = playlist.metadata.isShowingArchived

        return remember(playlistType, sortType, playlist.metadata) {
            val hasEpisodes = episodeCount > 0
            buildList {
                if (hasEpisodes) {
                    add(
                        PlaylistOption(
                            title = getString(LR.string.playlist_select_episodes),
                            iconId = IR.drawable.ic_playlists_select_episode,
                            onClick = {
                                viewModel.trackSelectEpisodesTapped()
                                viewModel.startMultiSelecting()
                                dismiss()
                            },
                        ),
                    )
                }
                add(
                    PlaylistOption(
                        title = getString(LR.string.playlist_sort_by),
                        description = sortTypeLabel,
                        iconId = IR.drawable.ic_playlists_sort,
                        onClick = {
                            viewModel.trackSortByTapped()
                            onClickChangeSortType()
                        },
                    ),
                )
                if (hasEpisodes && playlistType == Playlist.Type.Manual) {
                    add(
                        PlaylistOption(
                            title = getString(LR.string.playlist_edit_episodes),
                            iconId = IR.drawable.ic_playlist_edit,
                            onClick = {
                                viewModel.trackRearrangeEpisodesTapped()
                                dismiss()
                                val fragment = EditPlaylistFragment.newInstance(playlist.uuid)
                                (requireActivity() as FragmentHostListener).showModal(fragment)
                            },
                        ),
                    )
                }
                if (hasEpisodes) {
                    add(
                        PlaylistOption(
                            title = getString(LR.string.playlist_download_all),
                            iconId = IR.drawable.ic_playlists_download_all,
                            onClick = {
                                viewModel.trackDownloadAllTapped()
                                downloadAll(episodeCount)
                                dismiss()
                            },
                        ),
                    )
                }
                add(
                    PlaylistOption(
                        title = getString(LR.string.chromecast),
                        iconId = IR.drawable.ic_chrome_cast,
                        onClick = {
                            viewModel.trackChromeCastTapped()
                            viewModel.startChromeCast()
                            dismiss()
                        },
                    ),
                )
                if (hasEpisodes && playlistType == Playlist.Type.Manual) {
                    add(
                        if (isShowingArchived && archivedEpisodeCount == episodeCount) {
                            PlaylistOption(
                                title = getString(LR.string.playlist_unarchive_all),
                                iconId = IR.drawable.ic_unarchive,
                                onClick = {
                                    viewModel.trackUnarchiveAllTapped()
                                    viewModel.unarchiveAllEpisodes()
                                    dismiss()
                                },
                            )
                        } else {
                            PlaylistOption(
                                title = getString(LR.string.playlist_archive_all),
                                iconId = IR.drawable.ic_playlist_archive_all,
                                onClick = {
                                    viewModel.trackArchiveAllTapped()
                                    viewModel.archiveAllEpisodes()
                                    dismiss()
                                },
                            )
                        },
                    )
                }
                add(
                    PlaylistOption(
                        title = getString(LR.string.playlist_options),
                        iconId = IR.drawable.ic_playlists_options,
                        onClick = {
                            viewModel.trackFilterOptionsTapped()
                            viewModel.showSettings()
                            dismiss()
                        },
                    ),
                )
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

private val smartSortTypes = PlaylistEpisodeSortType.entries - PlaylistEpisodeSortType.DragAndDrop

private val Playlist.availableSortOptions
    get() = when (type) {
        Playlist.Type.Manual -> PlaylistEpisodeSortType.entries
        Playlist.Type.Smart -> smartSortTypes
    }
