package au.com.shiftyjelly.pocketcasts.profile

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatar
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ProfileHeader(
    state: ProfileHeaderState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
    config: ProfileHeaderConfig = ProfileHeaderConfig(),
) {
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalProfileHeader(
            state = state,
            config = config,
            modifier = modifier,
            onClick = onClick,
            onShareClick = onShareClick,
        )

        else -> VerticalProfileHeader(
            state = state,
            config = config,
            modifier = modifier,
            onClick = onClick,
            onShareClick = onShareClick,
        )
    }
}

data class ProfileHeaderState(
    val email: String?,
    val imageUrl: String?,
    val subscriptionTier: SubscriptionTier?,
    val expiresIn: Duration?,
    val isShareVisible: Boolean = false,
)

data class ProfileHeaderConfig(
    val infoFontScale: Float = 1f,
    val spacingScale: Float = 1f,
    val avatarConfig: UserAvatarConfig = UserAvatarConfig(),
)

@Composable
fun VerticalProfileHeader(
    state: ProfileHeaderState,
    config: ProfileHeaderConfig,
    onClick: () -> Unit,
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isSignedIn = state.email != null
    val accountLabel = if (isSignedIn) {
        stringResource(LR.string.account)
    } else {
        stringResource(LR.string.profile_set_up_account)
    }
    val context = LocalContext.current
    val expirationLabel = remember(state.expiresIn) {
        state.expiresIn?.let { duration -> expirationLabel(context, duration, state.subscriptionTier) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp * config.spacingScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            onClickLabel = accountLabel,
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ),
    ) {
        UserAvatar(
            imageUrl = state.imageUrl,
            subscriptionTier = state.subscriptionTier,
            borderCompletion = state.expiresIn?.let { it / 30.days }?.toFloat() ?: 1f,
            config = config.avatarConfig,
        )
        if (expirationLabel != null) {
            TextH70(
                text = expirationLabel.uppercase(),
                fontScale = config.infoFontScale,
                color = MaterialTheme.theme.colors.support05,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp * config.spacingScale),
            )
        }
        if (isSignedIn) {
            TextH50(
                text = state.email,
                fontScale = config.infoFontScale,
                textAlign = TextAlign.Center,
            )
        }
        ProfileHeaderActions(
            state = state,
            isSignedIn = isSignedIn,
            config = config,
            onClick = onClick,
            onShareClick = onShareClick,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 350.dp),

        )
    }
}

@Composable
fun HorizontalProfileHeader(
    state: ProfileHeaderState,
    config: ProfileHeaderConfig,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSignedIn = state.email != null
    val accountLabel = if (isSignedIn) {
        stringResource(LR.string.account)
    } else {
        stringResource(LR.string.profile_set_up_account)
    }
    val context = LocalContext.current
    val expirationLabel = remember(state.expiresIn) {
        state.expiresIn?.let { duration -> expirationLabel(context, duration, state.subscriptionTier) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.clickable(
            onClickLabel = accountLabel,
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ),
    ) {
        UserAvatar(
            imageUrl = state.imageUrl,
            subscriptionTier = state.subscriptionTier,
            borderCompletion = state.expiresIn?.let { it / 30.days }?.toFloat() ?: 1f,
            config = config.avatarConfig,
        )
        Spacer(
            modifier = Modifier.width(16.dp * config.spacingScale),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp * config.spacingScale),
            horizontalAlignment = Alignment.Start,
        ) {
            if (expirationLabel != null) {
                TextH70(
                    text = expirationLabel.uppercase(),
                    fontScale = config.infoFontScale,
                    color = MaterialTheme.theme.colors.support05,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp * config.spacingScale),
                )
            }
            if (isSignedIn) {
                TextH50(
                    text = state.email,
                    fontScale = config.infoFontScale,
                    textAlign = TextAlign.Center,
                )
            }
            ProfileHeaderActions(
                state = state,
                config = config,
                onClick = onClick,
                onShareClick = onShareClick,
                isSignedIn = isSignedIn,
            )
        }
    }
}

@Composable
private fun ProfileHeaderActions(
    state: ProfileHeaderState,
    isSignedIn: Boolean,
    config: ProfileHeaderConfig,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSignedIn) {
        val showShare = state.email != null && state.isShareVisible
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.padding(top = 8.dp * config.spacingScale),
        ) {
            ProfileButton(
                text = stringResource(LR.string.account),
                image = if (state.email != null) painterResource(IR.drawable.ic_profile_circle_solid) else null,
                onClick = onClick,
                config = config,
                modifier = if (showShare) Modifier.weight(1f) else Modifier,
            )
            if (showShare) {
                ProfileButton(
                    text = stringResource(LR.string.share),
                    image = painterResource(IR.drawable.ic_share),
                    onClick = onShareClick,
                    config = config,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        ProfileButton(
            text = stringResource(LR.string.profile_set_up_account),
            onClick = onClick,
            config = config,
        )
    }
}

@Composable
private fun ProfileButton(
    text: String,
    config: ProfileHeaderConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: Painter? = null,
) {
    OutlinedButton(
        border = ButtonDefaults.outlinedBorder.copy(
            brush = SolidColor(MaterialTheme.theme.colors.primaryUi05),
            width = 2.dp,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText01),
                    modifier = Modifier.size(18.dp),
                )
            }
            TextH40(
                text = text,
                fontScale = config.infoFontScale,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

private fun expirationLabel(context: Context, duration: Duration, subscriptionTier: SubscriptionTier?): String? {
    if (duration > 30.days) {
        return null
    }
    return when (subscriptionTier) {
        null -> null
        SubscriptionTier.Plus -> context.getString(LR.string.profile_plus_expires_in, duration.toExpirationString(context))
        SubscriptionTier.Patron -> context.getString(LR.string.profile_patron_expires_in, duration.toExpirationString(context))
    }
}

private fun Duration.toExpirationString(context: Context) = toFriendlyString(context.resources, maxPartCount = 1)

@Preview
@Composable
private fun ProfileHeaderUnsignedPreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = null,
                imageUrl = null,
                subscriptionTier = null,
                expiresIn = null,
                isShareVisible = false,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderFreePreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = null,
                expiresIn = null,
                isShareVisible = true,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderPatronPreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.Patron,
                expiresIn = 31.days,
                isShareVisible = false,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderPatronExpirePreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.Patron,
                expiresIn = 25.days,
                isShareVisible = true,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderPlusPreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.Plus,
                expiresIn = 31.days,
                isShareVisible = true,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderPlusExpirePreview() {
    PreviewBox(
        width = 300.dp,
    ) {
        ProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.Plus,
                expiresIn = 8.minutes,
                isShareVisible = true,
            ),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderHorizontalPreview() {
    PreviewBox(
        width = 500.dp,
        height = 200.dp,
    ) {
        HorizontalProfileHeader(
            state = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.Plus,
                expiresIn = 20.days,
                isShareVisible = true,
            ),
            config = ProfileHeaderConfig(),
            onClick = {},
            onShareClick = {},
        )
    }
}

@Composable
private fun PreviewBox(
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    content: @Composable () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width, height)
                .background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            content()
        }
    }
}
