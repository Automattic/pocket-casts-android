package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * The announcement shown over the main screen once, when Playback 2026 becomes available.
 */
@Composable
fun Playback2026LaunchBottomSheet(
    parent: ViewGroup,
    onClick: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        parent = parent,
        shouldShow = true,
        onExpand = onExpand,
        onCollapse = {},
        content = BottomSheetContentState.Content(
            imageContent = {
                Image(
                    painter = painterResource(IR.drawable.playback_2026_sheet),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
            },
            summaryText = stringResource(LR.string.playback_2026_sheet_description),
            primaryButton = BottomSheetContentState.Content.Button.Primary(
                label = stringResource(LR.string.playback_2026_sheet_button),
                onClick = onClick,
            ),
        ),
        modifier = modifier,
    )
}
