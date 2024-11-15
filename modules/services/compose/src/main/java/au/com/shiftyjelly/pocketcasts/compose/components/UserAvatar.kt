package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UserAvatar(
    imageUrl: String?,
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    borderCompletion: Float = 1f,
    config: UserAvatarConfig = UserUiDefaults.avatarConfig,
) {
    SubcomposeLayout(
        modifier = modifier,
    ) { constraints ->
        val image = subcompose("image") {
            UserImage(
                imageUrl = imageUrl,
                subscriptionTier = subscriptionTier,
                borderCompletion = borderCompletion,
                config = config.imageConfig,
            )
        }[0].measure(constraints)

        val badge = if (subscriptionTier == PATRON) {
            subcompose("badge") {
                UserBadge(
                    subscriptionTier = subscriptionTier,
                    config = config.badgeConfig,
                )
            }[0].measure(constraints)
        } else {
            null
        }

        val width = maxOf(image.width, badge?.width ?: 0)
        val height = image.height + (badge?.height ?: 0) / 2

        layout(width, height) {
            image.place(
                x = (width - image.width) / 2,
                y = 0,
            )
            if (badge != null) {
                badge.place(
                    x = (width - badge.width) / 2,
                    y = image.height - badge.height / 2,
                )
            }
        }
    }
}

@Composable
fun UserImage(
    imageUrl: String?,
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    borderCompletion: Float = 1f,
    config: UserImageConfig = UserUiDefaults.imageConfig,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(config.size)
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
                .size(config.size)
                .clip(CircleShape),
        )
        if (config.borderWidth > Dp.Hairline && subscriptionTier != NONE) {
            val borderColor = subscriptionTier.toDarkColor()
            Canvas(
                Modifier
                    .padding(config.borderWidth / 2)
                    .size(config.size + config.borderWidth * 2 + config.borderPadding * 2),
            ) {
                val borderWidthPx = config.borderWidth.toPx()
                drawArc(
                    color = borderColor,
                    startAngle = 270f,
                    sweepAngle = 360f * -borderCompletion,
                    useCenter = false,
                    style = Stroke(borderWidthPx),
                )
            }
        }
    }
}

@Composable
private fun UserBadge(
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    config: UserBadgeConfig = UserUiDefaults.badgeConfig,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .background(subscriptionTier.toDarkColor(), RoundedCornerShape(50))
            .padding(config.contentPadding),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_patron),
            tint = Color.White,
            contentDescription = null,
            modifier = Modifier.size(config.iconSize),
        )
        Spacer(
            modifier = Modifier.width(4.dp),
        )
        TextH50(
            text = stringResource(LR.string.pocket_casts_patron_short),
            color = Color.White,
            fontSize = config.fontSize,
            lineHeight = config.fontSize,
        )
    }
}

object UserUiDefaults {
    val imageConfig = UserImageConfig(
        size = 104.dp,
        borderPadding = 2.dp,
        borderWidth = 2.dp,
    )

    val badgeConfig = UserBadgeConfig(
        iconSize = 14.dp,
        fontSize = 14.sp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
    )

    val avatarConfig = UserAvatarConfig(
        imageConfig = imageConfig,
        badgeConfig = badgeConfig,
    )
}

data class UserImageConfig(
    val size: Dp,
    val borderWidth: Dp,
    val borderPadding: Dp,
)

data class UserBadgeConfig(
    val iconSize: Dp,
    val fontSize: TextUnit,
    val contentPadding: PaddingValues,
)

data class UserAvatarConfig(
    val imageConfig: UserImageConfig,
    val badgeConfig: UserBadgeConfig,
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
    PLUS -> Color.Black
    PATRON -> Color.White
}

private fun SubscriptionTier.toIconSize(config: UserImageConfig) = when (this) {
    NONE, PATRON -> DpSize(config.size / 3, config.size / 3)
    PLUS -> DpSize(config.size, config.size / 3)
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
