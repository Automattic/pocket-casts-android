package au.com.shiftyjelly.pocketcasts.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
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
internal fun FiltersTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(y = 6.dp, x = -16.dp)
                .align(Alignment.End)
                .size(16.dp)
                .background(
                    color = MaterialTheme.theme.colors.primaryUi01,
                    shape = UpwardTriangleShape,
                )
                .semantics {
                    invisibleToUser()
                },
        )

        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.theme.colors.primaryUi01,
                    shape = RoundedCornerShape(4.dp),
                )
                .semantics(mergeDescendants = true) {},
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.filters_tooltip_title),
                    disableAutoScale = true,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextP40(
                    text = stringResource(LR.string.filters_tooltip_subtitle),
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
    }
}

private object UpwardTriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        return Outline.Generic(
            Path().apply {
                val edgeLength = (size.height * 3 / 2) / sqrt(3f)
                val triangleHeight = edgeLength * sqrt(3f) / 2
                moveTo(size.width / 2, 0f)
                lineTo(0f, triangleHeight)
                lineTo(size.width, triangleHeight)
                close()
            },
        )
    }
}

@Preview
@Composable
private fun PodcastHeaderTooltipPreview() {
    AppTheme(Theme.ThemeType.INDIGO) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(16.dp),
        ) {
            FiltersTooltip(
                onClickClose = {},
            )
        }
    }
}
