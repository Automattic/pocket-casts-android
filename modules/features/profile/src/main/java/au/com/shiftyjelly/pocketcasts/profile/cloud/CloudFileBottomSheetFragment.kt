package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UploadedFileDetailModalOptionType
import com.automattic.eventhorizon.UserFileDetailDismissedEvent
import com.automattic.eventhorizon.UserFileDetailShownEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CloudFileBottomSheetFragment :
    BottomSheetDialogFragment(),
    CloudFileListener {
    companion object {
        private const val NEW_INSTANCE_ARG = "CloudFileBottomSheetArg"

        fun newInstance(
            userEpisodeId: String,
            forceDark: Boolean = false,
            source: EpisodeViewSource = EpisodeViewSource.UNKNOWN,
        ) = CloudFileBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_ARG,
                    Args(userEpisodeId, forceDark, source),
                )
            }
        }
    }

    @Parcelize
    private class Args(
        val episodeId: String,
        val forceDark: Boolean,
        val source: EpisodeViewSource,
    ) : Parcelable

    @Inject lateinit var settings: Settings

    @Inject lateinit var theme: Theme

    @Inject lateinit var warningsHelper: WarningsHelper

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var rowDataProvider: EpisodeRowDataProvider

    private var pocketCastsImageRequestFactory: PocketCastsImageRequestFactory? = null
    private val viewModel: CloudBottomSheetViewModel by viewModels()

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    val activeTheme: Theme.ThemeType
        get() = if (args.forceDark && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState == null) {
            eventHorizon.track(
                UserFileDetailShownEvent(
                    source = args.source.analyticsValue,
                ),
            )
        }
        if (!args.forceDark) {
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.setup(args.episodeId)
        return contentWithoutConsumedInsets {
            val state by remember { viewModel.state.asFlow() }.collectAsStateWithLifecycle(null)
            val signInState by remember { viewModel.signInState.asFlow() }.collectAsStateWithLifecycle(null)
            val currentState = state ?: return@contentWithoutConsumedInsets
            val episode = currentState.episode
            val rowData by remember(episode.uuid) { rowDataProvider.userEpisodeRowDataFlow(episode.uuid) }.collectAsStateWithLifecycle(null)
            val artwork = remember(episode) { pocketCastsImageRequestFactory?.create(episode, useEpisodeArtwork = false) } ?: return@contentWithoutConsumedInsets
            AppTheme(if (args.forceDark) Theme.ThemeType.DARK else theme.activeTheme) {
                CloudFilePage(
                    state = currentState,
                    rowData = rowData,
                    artwork = artwork,
                    cloudRowVisibility = when (val signIn = signInState) {
                        null -> CloudRowVisibility.Unknown
                        is SignInState.SignedIn -> if (signIn.subscription != null) CloudRowVisibility.Cloud else CloudRowVisibility.Locked
                        else -> CloudRowVisibility.Locked
                    },
                    listener = this,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)),
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        eventHorizon.track(UserFileDetailDismissedEvent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.skipCollapsed = true
        }
    }

    override fun onPlayClick(episode: UserEpisode) {
        viewModel.playNow(episode, false)
        dialog?.dismiss()
    }

    override fun onPauseClick() {
        viewModel.pause()
    }

    override fun onUpNextClick(episode: UserEpisode, inUpNext: Boolean) {
        if (inUpNext) {
            viewModel.removeFromUpNext(episode)
        } else {
            val upNextDialog = OptionsDialog()
                .setIconColor(ThemeColor.primaryIcon01(theme.activeTheme))
                .addCheckedOption(
                    LR.string.play_next,
                    imageId = IR.drawable.ic_upnext_playnext,
                    click = {
                        viewModel.playNext(episode)
                    },
                )
                .addCheckedOption(
                    LR.string.play_last,
                    imageId = IR.drawable.ic_upnext_playlast,
                    click = {
                        viewModel.playLast(episode)
                    },
                )
            activity?.supportFragmentManager?.let {
                upNextDialog.show(it, "upnext")
            }
        }
        dialog?.dismiss()
    }

    override fun onPlayedClick(episode: UserEpisode) {
        if (episode.isFinished) {
            viewModel.markAsUnplayed(episode)
        } else {
            viewModel.markAsPlayed(episode)
        }
        dialog?.dismiss()
    }

    override fun onBookmarksClick() {
        dialog?.dismiss()
        viewModel.trackOptionTapped(UploadedFileDetailModalOptionType.Bookmarks)
        BookmarksContainerFragment.newInstance(
            episodeUuid = args.episodeId,
            sourceView = SourceView.FILES,
        ).show(parentFragmentManager, "bookmarks_container")
    }

    override fun onEditClick(episode: UserEpisode) {
        activity?.let { activity ->
            viewModel.trackOptionTapped(UploadedFileDetailModalOptionType.Edit)
            val intent = AddFileActivity.newEditInstance(activity, episode.uuid)
            activity.startActivity(intent)

            dialog?.dismiss()
        }
    }

    override fun onCloudClick(episode: UserEpisode) {
        when (episode.serverStatus) {
            UserEpisodeServerStatus.LOCAL, UserEpisodeServerStatus.MISSING -> upload(episode, Network.isUnmeteredConnection(requireContext()))

            UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> viewModel.cancelUpload(episode)

            UserEpisodeServerStatus.UPLOADED -> if (episode.isDownloaded) {
                viewModel.removeEpisode(episode)
            } else if (episode.isDownloading) {
                viewModel.cancelDownload(episode)
            } else {
                download(episode, Network.isUnmeteredConnection(requireContext()))
            }
        }

        dialog?.dismiss()
    }

    override fun onLockedCloudClick() {
        viewModel.trackOptionTapped(UploadedFileDetailModalOptionType.UploadUpgradeRequired)
        OnboardingLauncher.openOnboardingFlow(
            requireActivity(),
            OnboardingFlow.Upsell(OnboardingUpgradeSource.FILES),
        )
    }

    override fun onDeleteClick(episode: UserEpisode) {
        val deleteState = viewModel.getDeleteStateOnDeleteClick(episode)
        val confirmationDialog = CloudDeleteHelper.getDeleteDialog(episode, deleteState, viewModel::deleteEpisode, resources)
        confirmationDialog.show(parentFragmentManager, "delete_confirm")
        dialog?.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        pocketCastsImageRequestFactory = PocketCastsImageRequestFactory(context).themed().smallSize()
    }

    override fun onDetach() {
        super.onDetach()
        pocketCastsImageRequestFactory = null
    }

    fun download(episode: UserEpisode, isOnWifi: Boolean) {
        viewModel.trackOptionTapped(UploadedFileDetailModalOptionType.Download)
        if (settings.warnOnMeteredNetwork.value && !isOnWifi) {
            warningsHelper.downloadWarning(args.episodeId, SourceView.FILES)
                .show(parentFragmentManager, "download_warning")
        } else {
            viewModel.download(episode)
        }
    }

    private fun upload(episode: UserEpisode, isOnWifi: Boolean) {
        viewModel.trackOptionTapped(UploadedFileDetailModalOptionType.Upload)
        if (settings.warnOnMeteredNetwork.value && !isOnWifi) {
            warningsHelper.uploadWarning(args.episodeId, source = SourceView.FILES)
                .show(parentFragmentManager, "upload_warning")
        } else {
            viewModel.uploadEpisode(episode)
        }
    }
}
