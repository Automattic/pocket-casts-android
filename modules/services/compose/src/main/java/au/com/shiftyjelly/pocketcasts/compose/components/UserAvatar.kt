package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier.NONE
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier.PATRON
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier.PLUS
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UserAvatar(
    imageUrl: String?,
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    borderCompletion: Float = 1f,
    showPatronBadge: Boolean = true,
    config: UserAvatarConfig = UserAvatarConfig(),
) {
    SubcomposeLayout(
        modifier = modifier,
    ) { constraints ->
        val picture = subcompose("picture") {
            UserPicture(
                imageUrl = imageUrl,
                subscriptionTier = subscriptionTier,
                config = config,
            )
        }[0].measure(constraints)

        val border = if (config.strokeWidth > Dp.Hairline && subscriptionTier != NONE) {
            subcompose("border") {
                UserPictureBorder(
                    borderCompletion = borderCompletion,
                    subscriptionTier = subscriptionTier,
                    config = config,
                )
            }[0].measure(constraints)
        } else {
            null
        }

        val badge = if (subscriptionTier == PATRON && showPatronBadge) {
            subcompose("badge") {
                SubscriptionBadge(
                    subscriptionTier = PATRON,
                    backgroundColor = subscriptionTier.toDarkColor(),
                    fontSize = config.badgeFontSize,
                    fontColor = Color.White,
                    iconSize = config.badgeIconSize,
                    iconColor = Color.White,
                    contentPadding = config.badgeContentPadding,
                )
            }[0].measure(constraints)
        } else {
            null
        }

        val pictureSize = IntSize(picture.width, picture.height)
        val borderSize = IntSize(border?.width ?: 0, border?.height ?: 0)
        val badgeSize = IntSize(badge?.width ?: 0, badge?.height ?: 0)

        val outerHeight = maxOf(pictureSize.height, borderSize.height)
        val badgeOffset = config.strokeWidth.roundToPx() * 2
        val width = maxOf(pictureSize.width, borderSize.width, badgeSize.width)
        val height = maxOf(outerHeight + badgeSize.height / 2 - badgeOffset, outerHeight)

        layout(width, height) {
            picture.place(
                x = (width - picture.width) / 2,
                y = if (borderSize.height > pictureSize.height) {
                    (borderSize.height - pictureSize.height) / 2
                } else {
                    0
                },
            )
            if (border != null) {
                border.place(0, 0)
            }
            if (badge != null) {
                badge.place(
                    x = (width - badge.width) / 2,
                    y = outerHeight - badge.height / 2 - badgeOffset,
                )
            }
        }
    }
}

@Composable
private fun UserPicture(
    imageUrl: String?,
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    config: UserAvatarConfig = UserAvatarConfig(),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(config.imageSize)
                .background(subscriptionTier.toBackgroundBrush(), CircleShape),
        ) {
            Image(
                painter = painterResource(subscriptionTier.toIcon()),
                contentDescription = null,
                colorFilter = ColorFilter.tint(subscriptionTier.toIconTint()),
                modifier = Modifier.size(subscriptionTier.toIconSize(config)),
            )
        }
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(config.imageSize)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun UserPictureBorder(
    borderCompletion: Float,
    config: UserAvatarConfig,
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
) {
    val borderColor = subscriptionTier.toDarkColor()
    Canvas(
        modifier
            .padding(config.strokeWidth / 2)
            .size(config.imageSize + config.strokeWidth + config.imageContentPadding * 2),
    ) {
        val borderWidthPx = config.strokeWidth.toPx()
        drawArc(
            color = borderColor,
            startAngle = 270f,
            sweepAngle = 360f * -borderCompletion,
            useCenter = false,
            style = Stroke(borderWidthPx, cap = StrokeCap.Round),
        )
    }
}

data class UserAvatarConfig(
    val imageSize: Dp = 104.dp,
    val imageContentPadding: Dp = 3.dp,
    val strokeWidth: Dp = 4.dp,
    val badgeFontSize: TextUnit = 12.sp,
    val badgeIconSize: Dp = 12.dp,
    val badgeContentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
)

@Composable
private fun SubscriptionTier.toLightColor() = when (this) {
    NONE -> Color.Transparent
    PLUS -> colorResource(UR.color.plus_gold_light)
    PATRON -> colorResource(UR.color.patron_purple_light)
}

@Composable
private fun SubscriptionTier.toDarkColor() = when (this) {
    NONE -> Color.Transparent
    PLUS -> colorResource(UR.color.plus_gold_dark)
    PATRON -> colorResource(UR.color.patron_purple)
}

@Composable
private fun SubscriptionTier.toBackgroundBrush() = when (this) {
    NONE -> SolidColor(MaterialTheme.theme.colors.primaryIcon02)
    PLUS, PATRON -> Brush.horizontalGradient(listOf(toLightColor(), toDarkColor()))
}

@Composable
private fun SubscriptionTier.toIcon() = when (this) {
    NONE, PATRON -> IR.drawable.ic_account_free
    PLUS -> IR.drawable.ic_account_plus
}

@Composable
private fun SubscriptionTier.toIconTint() = when (this) {
    NONE -> MaterialTheme.theme.colors.primaryUi01
    PLUS, PATRON -> if (MaterialTheme.theme.isLight) Color.White else Color.Black
}

private fun SubscriptionTier.toIconSize(config: UserAvatarConfig) = when (this) {
    NONE, PATRON -> DpSize(config.imageSize / 3, config.imageSize / 3)
    PLUS -> DpSize(config.imageSize, config.imageSize / 3)
}

@Preview
@Composable
private fun UserAvatarTierPreview(
    @PreviewParameter(SubscriptionTierParameterProvider::class) subscriptionTier: SubscriptionTier,
) {
    AppTheme(Theme.ThemeType.LIGHT) {
        UserAvatar(
            imageUrl = null,
            subscriptionTier = subscriptionTier,
        )
    }
}

@Preview
@Composable
private fun UserAvatarCompletionPreview(
    @PreviewParameter(BorderCompletionParameterProvider::class) borderCompletion: Float,
) {
    AppTheme(Theme.ThemeType.LIGHT) {
        UserAvatar(
            imageUrl = null,
            subscriptionTier = PLUS,
            borderCompletion = borderCompletion,
        )
    }
}

@Preview
@Composable
private fun UserAvatarThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppTheme(theme) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            UserAvatar(
                imageUrl = null,
                subscriptionTier = NONE,
            )
        }
    }
}

private class SubscriptionTierParameterProvider : PreviewParameterProvider<SubscriptionTier> {
    override val values = SubscriptionTier.entries.asSequence()
}

private class BorderCompletionParameterProvider : PreviewParameterProvider<Float> {
    override val values = sequenceOf(1f, 0.8f, 0.6f, 0.4f, 0.2f, 0f)
}
