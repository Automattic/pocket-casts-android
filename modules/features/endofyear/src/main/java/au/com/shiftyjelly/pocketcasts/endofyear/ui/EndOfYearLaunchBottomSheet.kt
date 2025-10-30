package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EndOfYearLaunchBottomSheet(
    parent: ViewGroup,
    onClick: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        parent = parent,
        sheetState = sheetState,
        shouldShow = shouldShow,
        onExpand = onExpand,
        content = BottomSheetContentState.Content(
            imageContent = {
                ImageContent(
                    modifier = modifier.clickable {
                        onClick()
                        scope.launch { sheetState.hide() }
                    },
                )
            },
            summaryText = stringResource(LR.string.end_of_year_launch_modal_summary),
            primaryButton = BottomSheetContentState.Content.Button.Primary(
                label = stringResource(LR.string.end_of_year_launch_modal_primary_button_title),
                onClick = onClick,
            ),
        ),
    )
}

@Composable
private fun ImageContent(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(IR.drawable.playback_launch_banner),
            contentDescription = null,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        )
    }
}
