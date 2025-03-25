package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.sqrt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastHeaderTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.background(
                color = MaterialTheme.theme.colors.primaryUi01,
                shape = RoundedCornerShape(4.dp),
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.podcast_header_redesign_title),
                    disableAutoScale = true,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextP40(
                    text = stringResource(LR.string.podcast_header_redesign_message),
                    color = MaterialTheme.theme.colors.primaryText02,
                    disableAutoScale = true,
                )
            }
            IconButton(
                onClick = onClickClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = stringResource(LR.string.close),
                    tint = MaterialTheme.theme.colors.primaryText01,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(y = -1.dp)
                .align(Alignment.CenterHorizontally)
                .background(MaterialTheme.theme.colors.primaryUi01, TriangleShape)
                .size(16.dp),
        )
    }
}

private object TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Generic(
        Path().apply {
            val edgeLength = (size.height * 3 / 2) / sqrt(3f)
            val triangleHeight = edgeLength * sqrt(3f) / 2
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, triangleHeight)
            close()
        },
    )
}

@Preview
@Composable
private fun PodcastHeaderTooltipPreview() {
    AppTheme(Theme.ThemeType.ELECTRIC) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(16.dp),
        ) {
            PodcastHeaderTooltip(
                onClickClose = {},
            )
        }
    }
}
