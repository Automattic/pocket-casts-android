package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRowTrailing
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectTopBar
import au.com.shiftyjelly.pocketcasts.views.compose.ToolbarCastButton
import au.com.shiftyjelly.pocketcasts.views.compose.UserEpisodeRow
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UploadedFilesAddFileTappedEvent
import com.automattic.eventhorizon.UploadedFilesModalOptionType
import com.automattic.eventhorizon.UploadedFilesMultiSelectEnteredEvent
import com.automattic.eventhorizon.UploadedFilesMultiSelectExitedEvent
import com.automattic.eventhorizon.UploadedFilesOptionsButtonTappedEvent
import com.automattic.eventhorizon.UploadedFilesOptionsModalOptionTappedEvent
import com.automattic.eventhorizon.UploadedFilesSelectAllAboveTappedEvent
import com.automattic.eventhorizon.UploadedFilesSelectAllBelowTappedEvent
import com.automattic.eventhorizon.UploadedFilesSelectAllTappedEvent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CloudFilesFragment : BaseFragment() {
    @Inject lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject lateinit var settings: Settings

    @Inject lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject lateinit var rowDataProvider: EpisodeRowDataProvider

    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory

    private val viewModel: CloudFilesViewModel by viewModels()
    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                factory.create(SwipeSource.Files, playlistUuid = null)
            }
        },
    )

    private var wasMultiSelecting = false

    private val onRowClick = { episode: BaseEpisode ->
        CloudFileBottomSheetFragment
            .newInstance(episode.uuid, source = EpisodeViewSource.FILES)
            .show(parentFragmentManager, "cloud_bottom_sheet")
    }

    private val pageListener = CloudFilesListener(
        onNavigationClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
        onOptionsClick = {
            eventHorizon.track(UploadedFilesOptionsButtonTappedEvent)
            showOptionsDialog()
        },
        onAddFileClick = {
            eventHorizon.track(UploadedFilesAddFileTappedEvent)
            startActivity(AddFileActivity.newFileChooser(requireContext()))
        },
        onRefresh = { viewModel.refreshFiles(userInitiated = true) },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val uiState by remember { viewModel.uiState.asFlow() }.collectAsStateWithLifecycle(null)
        val accountUsage by remember { viewModel.accountUsage.asFlow() }.collectAsStateWithLifecycle(null)
        val signInState by remember { viewModel.signInState.asFlow() }.collectAsStateWithLifecycle(null)
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(0)
        val controller = remember { MultiSelectController(multiSelectHelper, viewLifecycleOwner.lifecycleScope, SourceView.FILES) }
        val isMultiSelecting by controller.isMultiSelecting.collectAsStateWithLifecycle()
        AppTheme(theme.activeTheme) {
            CloudFilesPage(
                episodes = uiState?.userEpisodes,
                account = accountUsage?.get(),
                isRefreshEnabled = (signInState as? SignInState.SignedIn)?.subscription != null,
                bottomInsetPx = bottomInset,
                listener = pageListener,
                multiSelectTopBar = if (isMultiSelecting) {
                    { MultiSelectTopBar(controller, eventHorizon) }
                } else {
                    null
                },
                episodeRow = { episode -> FileRow(episode, controller, isMultiSelecting) },
                castButton = { tint -> ToolbarCastButton(tint, onClick = { chromeCastAnalytics.trackChromeCastViewShown() }) },
                toolbarModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }

    @Composable
    private fun FileRow(episode: UserEpisode, controller: MultiSelectController<BaseEpisode>, isMultiSelecting: Boolean) {
        val selected by controller.selected.collectAsStateWithLifecycle()
        UserEpisodeRow(
            episode = episode,
            rowDataProvider = rowDataProvider,
            imageRequestFactory = imageRequestFactory,
            swipeActions = { swipeRowActionsFactory.userEpisode(episode) },
            trailing = EpisodeRowTrailing.PlayButton(playButtonListener, eventHorizon),
            onClick = {
                if (multiSelectHelper.isMultiSelecting) {
                    multiSelectHelper.toggle(episode)
                } else {
                    onRowClick(episode)
                }
            },
            onLongClick = { multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = childFragmentManager) },
            onSwipeAction = { swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(
                        action = swipeAction,
                        episodeUuid = episode.uuid,
                        podcastUuid = episode.podcastOrSubstituteUuid,
                        fragmentManager = childFragmentManager,
                    )
                }
            },
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.Files),
            streamByDefault = settings.streamingMode.value,
            isMultiSelecting = isMultiSelecting,
            isSelected = selected.any { it.uuid == episode.uuid },
            animateMultiSelection = true,
        )
    }

    override fun onDestroyView() {
        multiSelectHelper.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        settings.trackingAutoPlaySource.set(AutoPlaySource.Predefined.Files, updateModifiedAt = false)
    }

    override fun onPause() {
        super.onPause()
        multiSelectHelper.isMultiSelecting = false
    }

    @SuppressLint("MissingSuperCall") // False positive
    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 4).smallSize().themed()

        playButtonListener.source = SourceView.FILES
        multiSelectHelper.source = SourceView.FILES
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.refreshFiles(userInitiated = false)

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            wasMultiSelecting = isMultiSelecting

            val event = if (isMultiSelecting) {
                UploadedFilesMultiSelectEnteredEvent
            } else {
                UploadedFilesMultiSelectExitedEvent
            }
            eventHorizon.track(event)
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    eventHorizon.track(
                        UploadedFilesSelectAllTappedEvent(
                            selectAll = true,
                        ),
                    )
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    multiSelectHelper.deselectAllInList(episodes)
                    eventHorizon.track(
                        UploadedFilesSelectAllTappedEvent(
                            selectAll = false,
                        ),
                    )
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                    }

                    eventHorizon.track(UploadedFilesSelectAllAboveTappedEvent)
                }
            }

            override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                    }

                    eventHorizon.track(UploadedFilesSelectAllBelowTappedEvent)
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                val cloudFiles = viewModel.uiState.value?.userEpisodes
                if (cloudFiles != null) {
                    val startIndex = cloudFiles.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesBelow = cloudFiles.subList(startIndex, cloudFiles.size)
                        multiSelectHelper.deselectAllInList(episodesBelow)
                    }
                }
            }

            override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
                val cloudFiles = viewModel.uiState.value?.userEpisodes
                if (cloudFiles != null) {
                    val startIndex = cloudFiles.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesAbove = cloudFiles.subList(0, startIndex + 1)
                        multiSelectHelper.deselectAllInList(episodesAbove)
                    }
                }
            }
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()
        multiSelectHelper.source = SourceView.FILES
    }

    private fun showOptionsDialog() {
        val dialog = OptionsDialog()
            .addTextOption(
                titleId = LR.string.sort_by,
                imageId = IR.drawable.ic_sort,
                click = {
                    eventHorizon.track(
                        UploadedFilesOptionsModalOptionTappedEvent(
                            option = UploadedFilesModalOptionType.SortBy,
                        ),
                    )
                    showSortOptions()
                },
            )
            .addTextOption(
                titleId = LR.string.profile_cloud_settings,
                imageId = IR.drawable.ic_profile_settings,
                click = {
                    eventHorizon.track(
                        UploadedFilesOptionsModalOptionTappedEvent(
                            option = UploadedFilesModalOptionType.FileSettings,
                        ),
                    )
                    showCloudSettings()
                },
            )
        dialog.show(parentFragmentManager, "cloud_options")
    }

    private fun showCloudSettings() {
        val settingsFragment = CloudSettingsFragment()
        (activity as FragmentHostListener).addFragment(settingsFragment)
    }

    private fun showSortOptions() {
        val dialog = OptionsDialog()
            .setTitle(getString(LR.string.sort_by))
            .addCheckedOption(
                titleId = LR.string.episode_sort_newest_to_oldest,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.NEWEST_OLDEST)
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.NEWEST_OLDEST),
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_oldest_to_newest,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.OLDEST_NEWEST)
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.OLDEST_NEWEST),
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_sort_by_title,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.A_TO_Z)
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.A_TO_Z),
            )
        dialog.show(parentFragmentManager, "sort_options")
    }

    override fun onBackPressed(): Boolean {
        return if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            true
        } else {
            false
        }
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val SELECT_ALL_KEY = "select_all"
        private const val SORT_BY = "sort_by"
        private const val FILE_SETTINGS = "file_settings"
    }
}
