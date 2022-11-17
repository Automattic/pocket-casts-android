package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ImageContentCornerRadius = 16.dp
private val ImageContentPadding = 20.dp

private val ImageContentCoverSize = 145.dp
private val ImageContentCoverCornerRadius = 8.dp
private val ImageContentCoverBottomPadding = 8.dp

@Composable
fun EndOfYearLaunchBottomSheet(
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
    onClick: () -> Unit,
    onExpanded: () -> Unit,
) {
    ModalBottomSheet(
        shouldShow = shouldShow,
        onExpanded = onExpanded,
        content = BottomSheetContentState.Content(
            titleText = stringResource(LR.string.end_of_year_launch_modal_title),
            imageContent = { ImageContent(modifier) },
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

@Composable
fun ImageContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ImageContentCornerRadius))
            .background(color = colorResource(R.color.modal_image_content_background_color))
            .padding(ImageContentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = modifier
                .size(ImageContentCoverSize)
                .clip(RoundedCornerShape(ImageContentCoverCornerRadius))
                .background(color = colorResource(R.color.black_26))
                .clearAndSetSemantics {},
            contentAlignment = Alignment.BottomCenter
        ) {
            Image(
                painter = painterResource(R.drawable.img_2022),
                contentDescription = null,
                modifier = modifier.fillMaxSize()
            )
            TextH70(
                text = stringResource(LR.string.end_of_year_launch_modal_image_title),
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = ImageContentCoverBottomPadding)
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
