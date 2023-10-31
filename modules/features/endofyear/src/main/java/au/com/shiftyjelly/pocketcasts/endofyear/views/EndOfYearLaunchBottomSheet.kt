package au.com.shiftyjelly.pocketcasts.endofyear.views

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ImageContentCornerRadius = 16.dp
private val ImageContentPadding = 16.dp

private val ImageContentCoverHeight = 190.dp
private val ImageContentCoverCornerRadius = 8.dp
private val BackgroundColor = Color(0xFF161718)

@Composable
fun EndOfYearLaunchBottomSheet(
    parent: ViewGroup,
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
    onClick: () -> Unit,
    onExpanded: () -> Unit,
) {
    ModalBottomSheet(
        parent = parent,
        shouldShow = shouldShow,
        onExpanded = onExpanded,
        content = BottomSheetContentState.Content(
            imageContent = { ImageContent(modifier) },
            summaryText = stringResource(LR.string.end_of_year_launch_modal_summary),
            primaryButton = BottomSheetContentState.Content.Button.Primary(
                label = stringResource(LR.string.end_of_year_launch_modal_primary_button_title),
                onClick = { onClick.invoke() }
            ),
        )
    )
}

@Composable
fun ImageContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ImageContentCornerRadius))
            .padding(horizontal = ImageContentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(ImageContentCoverHeight)
                .clip(RoundedCornerShape(ImageContentCoverCornerRadius))
                .background(BackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.img_2023_modal),
                contentDescription = stringResource(id = LR.string.end_of_year_pocket_casts_playback),
                modifier = modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ImageContent()
    }
}
