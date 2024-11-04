package au.com.shiftyjelly.pocketcasts.views.lowstorage

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LowStorageLaunchBottomSheet(
    parent: ViewGroup,
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
    totalDownloadSize: Long,
    onManageDownloadsClick: () -> Unit,
    onMaybeLaterClick: () -> Unit,
    onExpanded: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        parent = parent,
        shouldShow = shouldShow,
        customSheetState = sheetState,
        onExpanded = onExpanded,
        customContent = {
            LowStorageDialog(
                modifier = modifier,
                totalDownloadSize = totalDownloadSize,
                onManageDownloadsClick = {
                    coroutineScope.launch { sheetState.hide() }
                    onManageDownloadsClick.invoke()
                },
                onMaybeLaterClick = {
                    coroutineScope.launch { sheetState.hide() }
                    onMaybeLaterClick.invoke()
                },
            )
        },
    )
}

@Composable
internal fun LowStorageDialog(
    modifier: Modifier = Modifier,
    totalDownloadSize: Long,
    onManageDownloadsClick: () -> Unit,
    onMaybeLaterClick: () -> Unit,
) {
    val formattedTotalDownloadSize = Util.formattedBytes(bytes = totalDownloadSize, context = LocalContext.current)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(IR.drawable.swipe_affordance),
            contentDescription = stringResource(LR.string.drag_down_to_dismiss_content_description),
            tint = MaterialTheme.theme.colors.primaryIcon02,
            modifier = Modifier
                .width(56.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        )

        Icon(
            painterResource(IR.drawable.pencil_cleanup),
            contentDescription = stringResource(LR.string.pencil_clean_up_icon_content_description),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(40.dp),
            tint = MaterialTheme.theme.colors.primaryInteractive01,
        )

        TextH20(
            text = stringResource(LR.string.need_to_free_up_space),
            textAlign = TextAlign.Center,
            modifier = modifier.padding(bottom = 10.dp, start = 21.dp, end = 21.dp),
        )

        TextH50(
            text = stringResource(LR.string.save_space_by_managing_downloaded_episodes, formattedTotalDownloadSize),
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(bottom = 57.dp, start = 21.dp, end = 21.dp),
        )

        RowButton(
            text = stringResource(LR.string.manage_downloads),
            onClick = { onManageDownloadsClick.invoke() },
            includePadding = false,
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            ),
            modifier = modifier
                .padding(horizontal = 21.dp)
                .padding(bottom = 12.dp),
        )

        RowOutlinedButton(
            text = stringResource(LR.string.maybe_later),
            onClick = { onMaybeLaterClick.invoke() },
            includePadding = false,
            modifier = modifier
                .padding(bottom = 12.dp)
                .padding(horizontal = 21.dp),
            border = null,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLowStorageDialog(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        LowStorageDialog(
            totalDownloadSize = 1000000000,
            onManageDownloadsClick = {},
            onMaybeLaterClick = {},
        )
    }
}

interface LowStorageBottomSheetListener {
    fun showModal()
}
