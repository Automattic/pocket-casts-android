package au.com.shiftyjelly.pocketcasts.playlists.smart

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
internal class EditRulesFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<EditRulesViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<EditRulesViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            val navController = rememberNavController()
            val listener = rememberNavigationListener()

            ClearTransientRulesStateEffect(navController)

            AnimatedNonNullVisibility(
                item = viewModel.uiState.collectAsState().value,
                enter = fadeIn,
                exit = fadeOut,
            ) { uiState ->
                ManageSmartRulesPage(
                    playlistName = uiState.playlistTitle,
                    appliedRules = uiState.appliedRules,
                    rulesBuilder = uiState.rulesBuilder,
                    smartEpisodes = uiState.smartEpisodes,
                    smartStarredEpisodes = uiState.smartStarredEpisodes,
                    followedPodcasts = uiState.followedPodcasts,
                    totalEpisodeCount = uiState.totalEpisodeCount,
                    useEpisodeArtwork = uiState.useEpisodeArtwork,
                    navController = navController,
                    listener = listener,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Composable
    private fun rememberNavigationListener() = remember {
        object : ManageSmartRulesListener {
            override fun onChangeUseAllPodcasts(shouldUse: Boolean) = viewModel.useAllPodcasts(shouldUse)

            override fun onSelectPodcast(uuid: String) = viewModel.selectPodcast(uuid)

            override fun onDeselectPodcast(uuid: String) = viewModel.deselectPodcast(uuid)

            override fun onSelectAllPodcasts() = viewModel.selectAllPodcasts()

            override fun onDeselectAllPodcasts() = viewModel.deselectAllPodcasts()

            override fun onChangeUnplayedStatus(shouldUse: Boolean) = viewModel.useUnplayedEpisodes(shouldUse)

            override fun onChangeInProgressStatus(shouldUse: Boolean) = viewModel.useInProgressEpisodes(shouldUse)

            override fun onChangeCompletedStatus(shouldUse: Boolean) = viewModel.useCompletedEpisodes(shouldUse)

            override fun onUseReleaseDate(releaseDate: ReleaseDateRule) = viewModel.useReleaseDate(releaseDate)

            override fun onUseConstrainedDuration(shouldUse: Boolean) = viewModel.useConstrainedDuration(shouldUse)

            override fun onDecrementMinDuration() = viewModel.decrementMinDuration()

            override fun onIncrementMinDuration() = viewModel.incrementMinDuration()

            override fun onDecrementMaxDuration() = viewModel.decrementMaxDuration()

            override fun onIncrementMaxDuration() = viewModel.incrementMaxDuration()

            override fun onUseDownloadStatus(downloadStatus: DownloadStatusRule) = viewModel.useDownloadStatus(downloadStatus)

            override fun onUseMediaType(mediaType: MediaTypeRule) = viewModel.useMediaType(mediaType)

            override fun onUseStarredEpisodes(shouldUse: Boolean) = viewModel.useStarredEpisodes(shouldUse)

            override fun onApplyRule(rule: RuleType) = viewModel.applyRule(rule)

            override fun createPlaylistCallback() = null

            override fun onClose() = dismiss()
        }
    }

    @Composable
    private fun ClearTransientRulesStateEffect(navController: NavController) {
        var currentRoute by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                val newRoute = entry.destination.route
                if (currentRoute != newRoute) {
                    currentRoute = newRoute
                    viewModel.clearTransientRules()
                }
            }
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartRulesEditFragmentArgs"

        fun newInstance(playlistUuid: String) = EditRulesFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
