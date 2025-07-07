package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun PlusInterstitialStory(
    story: Story.PlusInterstitial,
    measurements: EndOfYearMeasurements,
    onClickUpsell: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        WaitSection(
            measurements = measurements,
        )
        PlusInfo(
            story = story,
            measurements = measurements,
            onClickUpsell = onClickUpsell,
        )
    }
}

@Composable
private fun ColumnScope.WaitSection(
    measurements: EndOfYearMeasurements,
) {
    val textFactory = rememberHumaneTextFactory(
        fontSize = 227.nonScaledSp * measurements.smallDeviceFactor,
    )

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .requiredWidth(measurements.width * 1.5f)
            .rotate(STORY_ROTATION_DEGREES),
    ) {
        WaitText(
            scrollDirection = HorizontalDirection.Left,
            textFactory = textFactory,
        )
        Spacer(
            modifier = Modifier.height(12.dp * measurements.smallDeviceFactor),
        )
        WaitText(
            scrollDirection = HorizontalDirection.Right,
            textFactory = textFactory,
        )
    }
}

@Composable
private fun WaitText(
    scrollDirection: HorizontalDirection,
    textFactory: HumaneTextFactory,
) {
    ScrollingRow(
        items = listOf("WAIT", "ATTENDEZ", "ESPERA", "ASPETTA", "AGARDA"),
        scrollDirection = scrollDirection,
    ) { text ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            textFactory.HumaneText(
                text = text,
                color = Color(0xFFF9BC48),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            Image(
                painter = painterResource(IR.drawable.eoy_plus_text_stop),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFFF9BC48)),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun PlusInfo(
    story: Story.PlusInterstitial,
    measurements: EndOfYearMeasurements,
    onClickUpsell: () -> Unit,
) {
    Column(
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.1f to story.backgroundColor,
            ),
        ),
    ) {
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_plus_interstitial_plus_badge),
            contentDescription = null,
            modifier = Modifier.padding(start = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH10(
            text = stringResource(LR.string.end_of_year_stories_theres_more),
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(LR.string.end_of_year_stories_subscribe_to_plus),
            fontSize = 15.sp,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        OutlinedEoyButton(
            text = stringResource(LR.string.eoy_story_stories_subscribe_to_plus_button_label),
            onClick = onClickUpsell,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PlusInterstitialPreview() {
    PreviewBox(currentPage = 7, progress = 1f) { measurements ->
        PlusInterstitialStory(
            story = Story.PlusInterstitial,
            measurements = measurements,
            onClickUpsell = {},
        )
    }
}
