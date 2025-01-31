package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastTooltip(
    title: String,
    subtitle: String,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tooltipColor = MaterialTheme.theme.colors.primaryUi01

    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = modifier.padding(24.dp).widthIn(max = 400.dp),
        ) {
            TooltipContent(tooltipColor, title, subtitle, onDismissRequest)

            TooltipArrow(tooltipColor)
        }
    }
}

@Composable
private fun TooltipContent(
    tooltipColor: Color,
    title: String,
    subtitle: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        backgroundColor = tooltipColor,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextH40(
                    text = title,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                TextH70(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(LR.string.close),
                tint = MaterialTheme.theme.colors.primaryIcon02,
                modifier = Modifier
                    .align(Alignment.Top)
                    .width(24.dp)
                    .clickable {
                        onDismissRequest.invoke()
                    },
            )
        }
    }
}

@Composable
private fun BoxScope.TooltipArrow(tooltipColor: Color) {
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 28.dp),
    ) {
        val triangleSize = 12.dp.toPx()

        drawPath(
            path = Path().apply {
                moveTo(0f, -2f)
                lineTo(triangleSize, 0f)
                lineTo(triangleSize / 2f, triangleSize)
                close()
            },
            color = tooltipColor,
        )
    }
}
