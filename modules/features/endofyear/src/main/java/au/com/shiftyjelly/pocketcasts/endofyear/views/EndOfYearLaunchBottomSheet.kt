package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EndOfYearLaunchBottomSheet(
    onClick: () -> Unit,
) {
    ModalBottomSheet(
        showOnLoad = true,
        content = BottomSheetContentState.Content(
            titleText = stringResource(LR.string.end_of_year_launch_modal_title),
            summaryText = stringResource(LR.string.end_of_year_launch_modal_summary),
            primaryButton = BottomSheetContentState.Content.Button.Primary(
                label = stringResource(LR.string.end_of_year_launch_modal_primary_button_title),
                onClick = { onClick.invoke() }
            ),
            secondaryButton = BottomSheetContentState.Content.Button.Secondary(
                label = stringResource(LR.string.end_of_year_launch_modal_secondary_button_title),
            ),
        )
    )
}
