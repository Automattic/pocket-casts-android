package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BestAppAnimation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalLogo()
        BestAppArtworkCollage(
            modifier = Modifier
                .weight(1f)
                .padding(top = 32.dp, bottom = 32.dp)
        )
        TextH10(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            text = stringResource(LR.string.onboarding_intro_carousel_best_app_title),
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextP40(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            text = stringResource(LR.string.onboarding_intro_carousel_pc_user),
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Composable
private fun BestAppArtworkCollage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Image
        },
        contentAlignment = Alignment.Center,
    ) {
        Layout(
            modifier = Modifier.height(240.dp),
            content = {
                Image(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_13),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(69.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_14),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(69.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_11),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_3),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(69.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_16),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_4),
                    contentDescription = ""
                )
                Image(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = RoundedCornerShape(4.dp)
                            clip = true
                        },
                    painter = painterResource(IR.drawable.artwork_15),
                    contentDescription = ""
                )
            }) { measurables, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {
                val placeables = measurables.map { it.measure(Constraints()) }

                placeables.forEachIndexed { index, placeable ->
                    when (index) {
                        0 -> placeable.placeRelative(
                            x = placeable.width / -2,
                            y = 90.dp.roundToPx(),
                        )

                        1 -> placeable.placeRelative(
                            x = 72.dp.roundToPx(),
                            y = 0
                        )

                        2 -> placeable.placeRelative(
                            x = 98.dp.roundToPx(),
                            y = constraints.maxHeight - placeable.height
                        )

                        3 -> placeable.placeRelative(
                            x = 121.dp.roundToPx(),
                            y = 36.dp.roundToPx(),
                            zIndex = 1f
                        )

                        4 -> placeable.placeRelative(
                            x = 242.dp.roundToPx(),
                            y = constraints.maxHeight - placeable.height - 20.dp.roundToPx()
                        )

                        5 -> placeable.placeRelative(
                            x = constraints.maxWidth - placeable.width - 27.dp.roundToPx(),
                            y = 0,
                            zIndex = 1f
                        )

                        6 -> placeable.placeRelative(
                            x = constraints.maxWidth - placeable.width / 2,
                            y = 90.dp.roundToPx()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationIsInsaneAnimation(modifier: Modifier = Modifier) {
}

@Composable
fun OrganizingPodcastsAnimation(modifier: Modifier = Modifier) {
}

@Preview
@Composable
private fun PreviewBestAppAnim() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    BestAppAnimation(modifier = Modifier.fillMaxWidth())
}