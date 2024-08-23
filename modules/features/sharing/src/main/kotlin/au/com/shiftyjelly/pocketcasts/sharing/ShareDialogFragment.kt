package au.com.shiftyjelly.pocketcasts.sharing

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.dialogs.OptionsDialogComponent
import au.com.shiftyjelly.pocketcasts.compose.dialogs.OptionsDialogOption
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.sharing.clip.ShareClipFragment
import au.com.shiftyjelly.pocketcasts.sharing.episode.ShareEpisodeFragment
import au.com.shiftyjelly.pocketcasts.sharing.podcast.SharePodcastFragment
import au.com.shiftyjelly.pocketcasts.sharing.timestamp.ShareEpisodeTimestampFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.utils.parceler.DurationParceler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.google.android.material.R as MR

@AndroidEntryPoint
class ShareDialogFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var theme: Theme

    @Inject
    lateinit var sharingClient: SharingClient

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val bottomSheet get() = requireDialog().findViewById<View>(MR.id.design_bottom_sheet)

    private val viewModel by viewModels<ShareViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareViewModel.Factory> { factory ->
                factory.create(args.podcast, args.episode, observePlayback = args.observePlayback)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val usePodcastColors = args.backgroundColor != null
        setContent {
            val state by viewModel.uiState.collectAsState()
            val podcast = state.podcast
            val episode = state.episode

            // Dismiss dialog as this is a user episode
            if (podcast == null || episode !is PodcastEpisode) {
                LaunchedEffect(Unit) { dismiss() }
                return@setContent
            }

            if (usePodcastColors) {
                val backgroundColor = Color(theme.playerBackgroundColor(podcast))
                val textColor = Color(ThemeColor.playerContrast01(theme.activeTheme))
                val dividerColor = textColor.copy(alpha = 0.4f)

                Box(modifier = Modifier.background(backgroundColor)) {
                    ShareDialog(
                        options = createShareOptions(podcast, episode, backgroundColor, textColor),
                        handleColor = Color(ThemeColor.primaryIcon02(theme.activeTheme)),
                        dividerColor = dividerColor,
                    )
                }
                LaunchedEffect(backgroundColor) {
                    refreshSystemColors(backgroundColor)
                }
            } else {
                AppTheme(theme.activeTheme) {
                    ShareDialog(
                        options = createShareOptions(podcast, episode),
                        handleColor = Color(ThemeColor.primaryIcon02(theme.activeTheme)),
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.backgroundColor?.let(::refreshSystemColors)
        view.doOnLayout {
            BottomSheetBehavior.from(bottomSheet).apply {
                state = STATE_EXPANDED
                peekHeight = 0
                skipCollapsed = true
            }
        }
    }

    @Composable
    private fun ShareDialog(
        options: List<OptionsDialogOption>,
        handleColor: Color,
        dividerColor: Color? = null,
    ) = Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(handleColor, RoundedCornerShape(2.dp)),
        )
        OptionsDialogComponent(
            options = options,
            dividerColor = dividerColor,
            title = null,
            iconColor = null,
        )
    }

    private fun createShareOptions(
        podcast: Podcast,
        episode: PodcastEpisode,
        backgroundColor: Color? = null,
        textColor: Color? = null,
    ) = buildList {
        if (ShareDialogFragment.Options.Podcast in args.options) {
            add(
                shareOption(
                    textId = LR.string.share_podcast,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    onClick = {
                        if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                            SharePodcastFragment
                                .newInstance(podcast, args.source)
                                .show(parentFragmentManager, "share_screen")
                        } else {
                            lifecycleScope.launch(NonCancellable) {
                                val request = SharingRequest.podcast(podcast)
                                    .setSourceView(args.source)
                                    .build()
                                sharingClient.share(request)
                            }
                        }
                    },
                ),
            )
        }
        if (ShareDialogFragment.Options.Episode in args.options) {
            add(
                shareOption(
                    textId = LR.string.podcast_share_episode,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    onClick = {
                        if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                            ShareEpisodeFragment
                                .newInstance(episode, podcast.backgroundColor, args.source)
                                .show(parentFragmentManager, "share_screen")
                        } else {
                            lifecycleScope.launch(NonCancellable) {
                                val request = SharingRequest.episode(podcast, episode)
                                    .setSourceView(args.source)
                                    .build()
                                sharingClient.share(request)
                            }
                        }
                    },
                ),
            )
            add(
                shareOption(
                    textId = LR.string.podcast_share_current_position,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    onClick = {
                        if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                            ShareEpisodeTimestampFragment
                                .forEpisodePosition(episode, podcast.backgroundColor, args.source)
                                .show(parentFragmentManager, "share_screen")
                        } else {
                            lifecycleScope.launch(NonCancellable) {
                                val request = SharingRequest.episodePosition(podcast, episode, episode.playedUpTo.seconds)
                                    .setSourceView(args.source)
                                    .build()
                                sharingClient.share(request)
                            }
                        }
                    },
                ),
            )
            if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                add(
                    shareOption(
                        textId = LR.string.podcast_share_clip,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        onClick = {
                            ShareClipFragment
                                .newInstance(episode, podcast.backgroundColor, args.source)
                                .show(parentFragmentManager, "share_screen")
                        },
                    ),
                )
            }
            if (episode.isDownloaded) {
                add(
                    shareOption(
                        textId = LR.string.podcast_share_open_file_in,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        onClick = {
                            lifecycleScope.launch(NonCancellable) {
                                val request = SharingRequest.episodeFile(podcast, episode)
                                    .setSourceView(args.source)
                                    .build()
                                sharingClient.share(request)
                            }
                        },
                    ),
                )
            }
        }
    }

    private fun shareOption(
        @StringRes textId: Int,
        textColor: Color?,
        backgroundColor: Color?,
        onClick: () -> Unit,
    ) = OptionsDialogOption(
        titleId = textId,
        titleColor = textColor?.toArgb(),
        backgroundColor = backgroundColor,
        click = {
            onClick()
            dismiss()
        },
    )

    private fun refreshSystemColors(color: Color) {
        val argbColor = color.toArgb()
        requireActivity().window?.let { activityWindow ->
            activityWindow.statusBarColor = argbColor
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = color.luminance() > 0.5f
        }
        requireDialog().window?.let { dialogWindow ->
            dialogWindow.navigationBarColor = argbColor
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = color.luminance() > 0.5f
        }
        bottomSheet.backgroundTintList = ColorStateList.valueOf(argbColor)
    }

    enum class Options {
        Podcast,
        Episode,
    }

    @Parcelize
    private class Args(
        val podcastUuid: String,
        val podcastTitle: String,
        val episodeUuid: String,
        val episodeTitle: String,
        @TypeParceler<Duration, DurationParceler>() val episodePlayedUpTo: Duration,
        val episodeFileType: String?,
        val episodeDownloadStatus: EpisodeStatusEnum,
        val episodeFilePath: String?,
        @TypeParceler<Color?, ColorParceler>() val backgroundColor: Color?,
        val options: List<ShareDialogFragment.Options>,
        val source: SourceView,
        val observePlayback: Boolean,
    ) : Parcelable {
        val podcast get() = Podcast(
            uuid = podcastUuid,
            title = podcastTitle,
        )

        val episode get() = PodcastEpisode(
            uuid = episodeUuid,
            title = episodeTitle,
            playedUpTo = episodePlayedUpTo.toDouble(DurationUnit.SECONDS),
            fileType = episodeFileType,
            episodeStatus = episodeDownloadStatus,
            downloadedFilePath = episodeFilePath,
            podcastUuid = podcastUuid,
            publishedDate = Date(0), // Dummy date, not imporatant for sharing,
        )
    }

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareFragmentArgs"

        fun newInstance(
            podcast: Podcast,
            episode: PodcastEpisode,
            source: SourceView,
            options: List<ShareDialogFragment.Options> = ShareDialogFragment.Options.entries,
            observePlayback: Boolean = false,
        ) = ShareDialogFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    podcastUuid = podcast.uuid,
                    podcastTitle = podcast.title,
                    episodeUuid = episode.uuid,
                    episodeTitle = episode.title,
                    episodePlayedUpTo = episode.playedUpTo.seconds,
                    episodeFileType = episode.fileType,
                    episodeDownloadStatus = episode.episodeStatus,
                    episodeFilePath = episode.downloadedFilePath,
                    backgroundColor = null,
                    options = options,
                    source = source,
                    observePlayback = observePlayback,
                ),
            )
        }

        fun newThemedInstance(
            podcast: Podcast,
            episode: PodcastEpisode,
            theme: Theme,
            source: SourceView,
            options: List<ShareDialogFragment.Options> = ShareDialogFragment.Options.entries,
            observePlayback: Boolean = true,
        ) = ShareDialogFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    podcastUuid = podcast.uuid,
                    podcastTitle = podcast.title,
                    episodeUuid = episode.uuid,
                    episodeTitle = episode.title,
                    episodePlayedUpTo = episode.playedUpTo.seconds,
                    episodeFileType = episode.fileType,
                    episodeDownloadStatus = episode.episodeStatus,
                    episodeFilePath = episode.downloadedFilePath,
                    backgroundColor = Color(theme.playerBackgroundColor(podcast)),
                    options = options,
                    source = source,
                    observePlayback = observePlayback,
                ),
            )
        }
    }
}
