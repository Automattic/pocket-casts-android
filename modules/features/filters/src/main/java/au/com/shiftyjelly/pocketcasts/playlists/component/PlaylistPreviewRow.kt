package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.TipPosition
import au.com.shiftyjelly.pocketcasts.compose.components.TooltipPopup
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist.Type
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaylistPreviewRow(
    playlist: PlaylistPreview,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    showPremadeTooltip: Boolean,
    showRearrangeTooltip: Boolean,
    onDismissTooltip: (PlaylistTooltip) -> Unit,
    showDivider: Boolean,
    onClick: () -> Unit,
    onDelete: (AnchoredDraggableState<SwipeToDeleteAnchor>) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.theme.colors.primaryUi01,
) {
    val artworkUuids by remember(playlist.uuid) {
        getArtworkUuidsFlow(playlist.uuid)
    }.collectAsState()

    val episodeCount by remember(playlist.uuid) {
        getEpisodeCountFlow(playlist.uuid)
    }.collectAsState()

    LaunchedEffect(playlist.uuid, refreshArtworkUuids) {
        refreshArtworkUuids(playlist.uuid)
    }

    LaunchedEffect(playlist.uuid, refreshEpisodeCount) {
        refreshEpisodeCount(playlist.uuid)
    }

    Box(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        val draggableState = remember {
            AnchoredDraggableState(
                initialValue = SwipeToDeleteAnchor.Resting,
            )
        }
        val density = LocalDensity.current
        val windowWidth = LocalWindowInfo.current.containerSize.width
        val draggableAnchors = remember(windowWidth, density) {
            val deleteIconSize = density.run { 24.dp.toPx() }
            val deleteIconPadding = density.run { 48.dp.toPx() }
            val deleteActionWidth = deleteIconSize + deleteIconPadding
            val componentWidth = windowWidth.toFloat()

            DraggableAnchors {
                SwipeToDeleteAnchor.Resting at 0f
                SwipeToDeleteAnchor.ShowDelete at -deleteActionWidth
                // Multiplied by 2 due to https://issuetracker.google.com/issues/367660226
                SwipeToDeleteAnchor.Delete at -componentWidth * 2
            }
        }

        SideEffect {
            draggableState.updateAnchors(draggableAnchors)
        }
        LaunchedEffect(draggableState, onDelete) {
            snapshotFlow { draggableState.settledValue }.collectLatest { deleteAnchor ->
                when (deleteAnchor) {
                    SwipeToDeleteAnchor.ShowDelete -> Unit
                    SwipeToDeleteAnchor.Delete -> onDelete(draggableState)
                    SwipeToDeleteAnchor.Resting -> Unit
                }
            }
        }

        CompositionLocalProvider(
            LocalRippleConfiguration provides DeleteRippleConfiguration,
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color(0xFFFF4539))
                    .fillMaxHeight()
                    .clickable(
                        role = Role.Button,
                        onClick = { onDelete(draggableState) },
                        enabled = draggableState.currentValue != SwipeToDeleteAnchor.Delete,
                    ),
            ) {
                Spacer(
                    modifier = Modifier.weight(1f),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset {
                            val deleteActionWidth = draggableAnchors.positionOf(SwipeToDeleteAnchor.ShowDelete).absoluteValue
                            val maxOffset = (deleteActionWidth - windowWidth.toFloat()) / 2
                            val dragOffset = draggableState.requireOffset()
                            IntOffset(
                                x = (deleteActionWidth + dragOffset)
                                    .coerceAtLeast(maxOffset)
                                    .roundToInt(),
                                y = 0,
                            )
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_delete),
                        tint = Color.White,
                        contentDescription = stringResource(LR.string.delete_playlist),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = draggableState.requireOffset().roundToInt(),
                        y = 0,
                    )
                }
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = draggableState.currentValue != SwipeToDeleteAnchor.Delete,
                )
                .background(backgroundColor)
                .semantics(mergeDescendants = true) {},
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Box {
                    PlaylistArtwork(
                        podcastUuids = artworkUuids.orEmpty(),
                        artworkSize = 56.dp,
                    )
                    if (showPremadeTooltip) {
                        TooltipPopup(
                            title = stringResource(LR.string.premade_playlists_tooltip_title),
                            body = stringResource(LR.string.premade_playlists_tooltip_body),
                            tipPosition = TipPosition.TopStart,
                            maxWidthFraction = 0.75f,
                            maxWidth = 400.dp,
                            elevation = 8.dp,
                            anchorOffset = DpOffset(x = (-8).dp, y = 4.dp),
                            onClick = { onDismissTooltip(PlaylistTooltip.Premade) },
                        )
                    }
                }
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    TextH40(
                        text = playlist.title,
                    )
                    if (playlist.type == Type.Smart) {
                        TextP50(
                            text = stringResource(LR.string.smart_playlist),
                            color = MaterialTheme.theme.colors.primaryText02,
                        )
                    }
                }
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                TextP50(
                    text = episodeCount?.toString().orEmpty(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
                Box {
                    Image(
                        painter = painterResource(IR.drawable.ic_chevron_small_right),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
                        modifier = Modifier
                            .padding(3.dp)
                            .size(24.dp),
                    )
                    if (showRearrangeTooltip) {
                        TooltipPopup(
                            title = stringResource(LR.string.rearrange_playlists_tooltip_title),
                            body = stringResource(LR.string.rearrange_playlists_tooltip_body),
                            tipPosition = TipPosition.TopEnd,
                            maxWidthFraction = 0.75f,
                            maxWidth = 400.dp,
                            elevation = 8.dp,
                            onClick = { onDismissTooltip(PlaylistTooltip.Rearrange) },
                        )
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(startIndent = 16.dp)
            }
        }
    }
}

internal enum class PlaylistTooltip {
    Premade,
    Rearrange,
}

internal enum class SwipeToDeleteAnchor {
    Resting,
    ShowDelete,
    Delete,
}

private val DeleteRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.15f,
        focusedAlpha = 0.15f,
        hoveredAlpha = 0.2f,
        pressedAlpha = 0.4f,
    ),
)

@Preview
@Composable
private fun PlaylistPreviewRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            PlaylistPreviewRow(
                playlist = SmartPlaylistPreview(
                    uuid = "",
                    title = "New Releases",
                    settings = Playlist.Settings.ForPreview,
                    smartRules = SmartRules.Default,
                    icon = PlaylistIcon(0),
                ),
                getArtworkUuidsFlow = { MutableStateFlow(null) },
                getEpisodeCountFlow = { MutableStateFlow(null) },
                refreshArtworkUuids = {},
                refreshEpisodeCount = {},
                showPremadeTooltip = false,
                showRearrangeTooltip = false,
                onDismissTooltip = {},
                showDivider = true,
                onClick = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = ManualPlaylistPreview(
                    uuid = "",
                    title = "In progress",
                    settings = Playlist.Settings.ForPreview,
                    icon = PlaylistIcon(0),
                ),
                getArtworkUuidsFlow = { MutableStateFlow(listOf("id-1")) },
                getEpisodeCountFlow = { MutableStateFlow(1) },
                refreshArtworkUuids = {},
                refreshEpisodeCount = {},
                showPremadeTooltip = false,
                showRearrangeTooltip = false,
                onDismissTooltip = {},
                showDivider = true,
                onClick = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = SmartPlaylistPreview(
                    uuid = "",
                    title = "Starred",
                    settings = Playlist.Settings.ForPreview,
                    smartRules = SmartRules.Default,
                    icon = PlaylistIcon(0),
                ),
                getArtworkUuidsFlow = { MutableStateFlow(List(4) { "id-$it" }) },
                getEpisodeCountFlow = { MutableStateFlow(null) },
                refreshArtworkUuids = {},
                refreshEpisodeCount = {},
                showPremadeTooltip = false,
                showRearrangeTooltip = false,
                onDismissTooltip = {},
                showDivider = false,
                onClick = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
