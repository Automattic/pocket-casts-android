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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import kotlin.math.roundToLong
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun EndingStory(
    story: Story.Ending,
    measurements: EndOfYearMeasurements,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        ByeByeSection(
            measurements = measurements,
        )
        EndingInfo(
            story = story,
        )
    }
}

@Composable
private fun ColumnScope.ByeByeSection(
    measurements: EndOfYearMeasurements,
) {
    val textFactory = rememberHumaneTextFactory(
        fontSize = 227.nonScaledSp * measurements.smallDeviceFactor,
    )

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .requiredWidth(measurements.width * 1.5f),
    ) {
        ByeByeText(
            scrollDirection = ScrollDirection.Left,
            textFactory = textFactory,
        )
        Spacer(
            modifier = Modifier.height(12.dp * measurements.smallDeviceFactor),
        )
        ByeByeText(
            scrollDirection = ScrollDirection.Right,
            textFactory = textFactory,
        )
    }
}

@Composable
private fun ByeByeText(
    scrollDirection: ScrollDirection,
    textFactory: HumaneTextFactory,
) {
    ScrollingRow(
        scrollDelay = { (20 / it.density).roundToLong().coerceAtLeast(4L) },
        items = listOf("THANKS", "MERCI", "GRACIAS", "OBRIGADO", "GRATKI"),
        scrollDirection = scrollDirection,
    ) { text ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            textFactory.HumaneText(
                text = text,
                color = Color(0xFFEEB1F4),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            Image(
                painter = painterResource(IR.drawable.eoy_heart_text_stop),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFFEEB1F4)),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun EndingInfo(
    story: Story.Ending,
) {
    Column(
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.1f to story.backgroundColor,
            ),
        ),
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH10(
            text = stringResource(LR.string.end_of_year_story_epilogue_title, 2025),
            disableScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(LR.string.end_of_year_story_epilogue_subtitle),
            fontSize = 15.sp,
            disableScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        OutlinedEoyButton(
            text = stringResource(LR.string.end_of_year_replay),
            onClick = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun EndingPreview() {
    PreviewBox { measurements ->
        EndingStory(
            story = Story.Ending,
            measurements = measurements,
        )
    }
}
