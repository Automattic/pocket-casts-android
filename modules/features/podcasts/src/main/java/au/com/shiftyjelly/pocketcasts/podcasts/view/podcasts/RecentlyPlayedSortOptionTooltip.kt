package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.sqrt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun RecentlyPlayedSortOptionTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 326.dp)
            .padding(horizontal = 8.dp)
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = {}, // Prevent clicks from propagating to overlay
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Triangle pointer
        Box(
            modifier = Modifier
                .offset(y = 6.dp, x = -(8).dp)
                .align(Alignment.End)
                .size(16.dp)
                .background(
                    color = MaterialTheme.theme.colors.primaryUi01,
                    shape = TriangleShape,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.podcasts_sort_by_tooltip_title),
                    disableAutoScale = true,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextP40(
                    text = stringResource(LR.string.podcasts_sort_by_tooltip_message),
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

private object TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
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

@Preview
@Composable
private fun TooltipPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        RecentlyPlayedSortOptionTooltip(
            onClickClose = {},
        )
    }
}
