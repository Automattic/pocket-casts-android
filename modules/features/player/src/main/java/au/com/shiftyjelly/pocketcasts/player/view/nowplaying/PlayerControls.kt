package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedPlayPauseButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlayerControls(
    playerColors: PlayerColors,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val playerControlsData by remember {
        playerViewModel.listDataLive
            .map {
                PlayerControlsData(
                    it.podcastHeader.isPlaying,
                    it.podcastHeader.skipBackwardInSecs.toDuration(DurationUnit.SECONDS),
                    it.podcastHeader.skipForwardInSecs.toDuration(DurationUnit.SECONDS),
                )
            }
    }.observeAsState(PlayerControlsData())

    Content(
        playerColors = playerColors,
        playerControlsData = playerControlsData,
        onPlayPauseClick = { playerViewModel.onPlayPauseClicked() },
        onSkipForwardClick = { playerViewModel.onSkipForwardClick() },
        onSkipBackClick = { playerViewModel.onSkipBackwardClick() },
        onSkipForwardLongPress = { playerViewModel.onSkipForwardLongClick() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Content(
    playerControlsData: PlayerControlsData,
    onPlayPauseClick: () -> Unit,
    onSkipForwardClick: () -> Unit,
    onSkipBackClick: () -> Unit,
    onSkipForwardLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    playerColors: PlayerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault(),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkipButton(
            skipDuration = playerControlsData.skipBackInSecs,
            contentDescription = stringResource(LR.string.skip_back),
            tintColor = playerColors.contrast01,
            onClick = onSkipBackClick,
        )

        AnimatedPlayPauseButton(
            isPlaying = playerControlsData.playing,
            onClick = onPlayPauseClick,
            iconTint = playerColors.background01,
            circleColor = playerColors.contrast01,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        SkipButton(
            skipDuration = playerControlsData.skipForwardInSecs,
            scaleX = -1f,
            contentDescription = stringResource(LR.string.skip_forward),
            tintColor = playerColors.contrast01,
            onClick = onSkipForwardClick,
            onLongClick = onSkipForwardLongPress,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SkipButton(
    skipDuration: Duration,
    contentDescription: String,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleX: Float = 1f,
    onLongClick: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.skip_button))
    val lottieAnimatable = rememberLottieAnimatable()
    val coroutineScope = rememberCoroutineScope()

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(tintColor.toArgb()),
            keyPath = arrayOf("**"),
        ),
    )

    CompositionLocalProvider(
        LocalRippleConfiguration provides
            RippleConfiguration(
                color = Color.White,
                rippleAlpha = RippleDefaults.rippleAlpha(Color.White, true),
            ),
    ) {
        Box(
            modifier = modifier
                .size(80.dp)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = {
                        onClick()
                        coroutineScope.launch {
                            lottieAnimatable.animate(composition)
                        }
                    },
                    onLongClick = {
                        onLongClick?.let {
                            it.invoke()
                            coroutineScope.launch {
                                lottieAnimatable.animate(composition)
                            }
                        }
                    },
                )
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            LottieAnimation(
                composition = composition,
                progress = { lottieAnimatable.progress },
                dynamicProperties = dynamicProperties,
                modifier = Modifier
                    .scale(scaleX = scaleX, scaleY = 1f),
            )

            TextH40(
                text = "${skipDuration.inWholeSeconds}",
                color = tintColor,
                modifier = Modifier
                    .padding(top = 8.dp),
            )
        }
    }
}

private data class PlayerControlsData(
    val playing: Boolean = false,
    val skipBackInSecs: Duration = 30.toDuration(DurationUnit.SECONDS),
    val skipForwardInSecs: Duration = 15.toDuration(DurationUnit.SECONDS),
)

@Preview
@Composable
private fun PlayerControlsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppTheme(theme) {
        Content(
            playerControlsData = PlayerControlsData(),
            onPlayPauseClick = {},
            onSkipForwardClick = {},
            onSkipBackClick = {},
            onSkipForwardLongPress = {},
        )
    }
}
