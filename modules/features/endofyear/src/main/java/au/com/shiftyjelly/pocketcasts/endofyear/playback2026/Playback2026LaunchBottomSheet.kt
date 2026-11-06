package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearLaunchBottomSheet
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun Playback2026LaunchBottomSheet(
    parent: ViewGroup,
    onClick: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EndOfYearLaunchBottomSheet(
        parent = parent,
        imageId = IR.drawable.playback_2026_sheet,
        summary = stringResource(LR.string.playback_2026_sheet_description),
        buttonLabel = stringResource(LR.string.playback_2026_sheet_button),
        onClick = onClick,
        onExpand = onExpand,
        onCollapse = {},
        modifier = modifier,
    )
}
