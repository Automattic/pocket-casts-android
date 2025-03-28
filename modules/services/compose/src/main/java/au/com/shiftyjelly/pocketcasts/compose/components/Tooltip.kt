package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.sqrt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Tooltip(
    title: String,
    message: String,
    onClickClose: () -> Unit,
    triangleHorizontalAlignment: Alignment.Horizontal,
    triangleDirection: TriangleDirection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 326.dp)
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = {},
            ),
    ) {
        val triangleOffset = when (triangleDirection) {
            TriangleDirection.Up -> DpOffset(-(8).dp, 6.dp)
            TriangleDirection.Down -> DpOffset(0.dp, -(1).dp)
        }
        val triangleComposable = @Composable {
            Box(
                modifier = Modifier
                    .offset(x = triangleOffset.x, y = triangleOffset.y)
                    .align(triangleHorizontalAlignment)
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.theme.colors.primaryUi01,
                        shape = TriangleShape(direction = triangleDirection),
                    )
                    .semantics {
                        invisibleToUser()
                    },
            )
        }
        if (triangleDirection == TriangleDirection.Up) {
            triangleComposable()
        }

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
                    text = title,
                    disableAutoScale = true,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextP40(
                    text = message,
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
        if (triangleDirection == TriangleDirection.Down) {
            triangleComposable()
        }
    }
}

private class TriangleShape(val direction: TriangleDirection) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Generic(
        Path().apply {
            val edgeLength = (size.height * 3 / 2) / sqrt(3f)
            val triangleHeight = edgeLength * sqrt(3f) / 2
            when (direction) {
                TriangleDirection.Up -> {
                    moveTo(size.width / 2, 0f)
                    lineTo(0f, triangleHeight)
                    lineTo(size.width, triangleHeight)
                    close()
                }
                TriangleDirection.Down -> {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, triangleHeight)
                    close()
                }
            }
        },
    )
}

enum class TriangleDirection {
    Up,
    Down,
}

@Preview
@Composable
private fun TooltipUpPreview() {
    AppTheme(ThemeType.DARK) {
        Tooltip(
            title = "Title",
            message = "This is a tooltip!",
            onClickClose = {},
            triangleHorizontalAlignment = Alignment.End,
            triangleDirection = TriangleDirection.Up,
            modifier = Modifier,
        )
    }
}

@Preview
@Composable
private fun TooltipDownPreview() {
    AppTheme(ThemeType.LIGHT) {
        Tooltip(
            title = "Title",
            message = "This is a tooltip!",
            onClickClose = {},
            triangleHorizontalAlignment = Alignment.CenterHorizontally,
            triangleDirection = TriangleDirection.Down,
            modifier = Modifier,
        )
    }
}
