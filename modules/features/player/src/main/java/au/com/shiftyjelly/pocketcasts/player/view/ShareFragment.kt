package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as IR

@AndroidEntryPoint
class ShareFragment : BaseDialogFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    override val statusBarColor: StatusBarColor? = null
    private lateinit var composeView: ComposeView
    private val viewModel: PlayerViewModel by activityViewModels()
    private var disposable: Disposable? = null
    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        composeView.setContent {
            val podcast = viewModel.podcast
            val episode = viewModel.episode
            val listData = viewModel.listDataLive.asFlow().collectAsState(initial = null)
            listData.value?.let {
                ShareScreen(
                    podcast = viewModel.podcast,
                    episode = viewModel.episode,
                    backgroundColor = Color(it.podcastHeader.backgroundColor),
                    onSharePodcast = {
                        if (podcast != null) {
                            SharePodcastHelper(
                                podcast,
                                null,
                                null,
                                requireContext(),
                                SharePodcastHelper.ShareType.PODCAST,
                                SourceView.PLAYER,
                                analyticsTracker
                            ).showShareDialogDirect()
                        }
                    },
                    onShareEpisode = {
                        if (podcast != null && episode is PodcastEpisode) {
                            SharePodcastHelper(
                                podcast,
                                episode,
                                null,
                                requireContext(),
                                SharePodcastHelper.ShareType.EPISODE,
                                SourceView.PLAYER,
                                analyticsTracker
                            ).showShareDialogDirect()
                        }
                    },
                    onShareCurrentPosition = {
                        if (podcast != null && episode is PodcastEpisode) {
                            SharePodcastHelper(
                                podcast,
                                episode,
                                episode.playedUpTo,
                                requireContext(),
                                SharePodcastHelper.ShareType.CURRENT_TIME,
                                SourceView.PLAYER,
                                analyticsTracker
                            ).showShareDialogDirect()
                        }
                    },
                    onShareOpenFileIn ={
                        if (podcast != null && episode is PodcastEpisode) {
                            SharePodcastHelper(
                                podcast,
                                episode,
                                episode.playedUpTo,
                                requireContext(),
                                SharePodcastHelper.ShareType.EPISODE_FILE,
                                SourceView.PLAYER,
                                analyticsTracker
                            ).sendFile()
                        }
                    }
                )
            }
            viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
                applyColor(theme, backgroundColor)
            }
        }
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

}

@Composable
private fun ShareScreen(
    podcast: Podcast?,
    episode: BaseEpisode?,
    backgroundColor: Color,
    onSharePodcast:()->Unit,
    onShareEpisode:()->Unit,
    onShareCurrentPosition:()->Unit,
    onShareOpenFileIn:()->Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
        ){
            Pill(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            if (podcast !=null){
                ShareButton(
                    modifier = Modifier
                        .padding(top = 20.dp)
                    ,
                    label = IR.string.share_podcast,
                    onClick = onSharePodcast
                )
            }
            Divider(
                color = Color.LightGray.copy(0.3f),
                thickness = 1.dp,
            )
            Divider(thickness = 1.dp)
            if (podcast !=null && episode is BaseEpisode){
                ShareButton(
                    label = IR.string.podcast_share_episode,
                    onClick = onShareEpisode
                )
                Divider(
                    color = Color.LightGray.copy(0.3f),
                    thickness = 1.dp,
                )
                ShareButton(
                    label = IR.string.podcast_share_current_position,
                    onClick = onShareCurrentPosition
                )
            }
            if (podcast !=null && episode is BaseEpisode && episode.isDownloaded){
                ShareButton(label = IR.string.podcast_share_open_file_in, onClick = onShareOpenFileIn)
            }
        }
    }
}

@Composable
private fun ShareButton(
    label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier
        .fillMaxWidth()
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(color = Color.LightGray)
        ) { onClick() }
        .height(64.dp)
        .padding(start = 32.dp)
        .wrapContentSize(Alignment.CenterStart)
    ){
        Text(
            text = stringResource(id = label),
            color = MaterialTheme.theme.colors.playerContrast01,
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp
        )
    }
}

private val PillSize = DpSize(width = 48.dp, height = 4.dp)
private val PillCornerRadius = 10.dp

@Composable
private fun Pill(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(PillSize)
            .clip(RoundedCornerShape(PillCornerRadius))
            .background(MaterialTheme.theme.colors.primaryText02)
    )
}

@Preview
@Composable
private fun ShareScreenPrev(
    theme: Theme.ThemeType = Theme.ThemeType.DARK
) {
    AppTheme(themeType = theme) {
        ShareScreen(
            podcast =Podcast() ,
            episode = null,
            backgroundColor = Color.DarkGray,
            onSharePodcast = {},
            onShareEpisode = { },
            onShareCurrentPosition = {},
            onShareOpenFileIn = {}
        )
    }
}