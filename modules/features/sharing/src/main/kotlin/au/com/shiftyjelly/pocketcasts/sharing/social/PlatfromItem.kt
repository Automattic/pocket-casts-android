package au.com.shiftyjelly.pocketcasts.sharing.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors

@Composable
internal fun PlatformItem(
    platform: SocialPlatform,
    shareColors: ShareColors,
    onClick: (SocialPlatform) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember(::MutableInteractionSource),
            indication = rememberRipple(color = shareColors.base),
            onClickLabel = stringResource(platform.nameId),
            role = Role.Button,
            onClick = { onClick(platform) },
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(shareColors.socialButton, CircleShape)
                .size(48.dp),
        ) {
            Image(
                painter = painterResource(platform.logoId),
                colorFilter = ColorFilter.tint(shareColors.socialButtonIcon),
                contentDescription = stringResource(platform.nameId),
            )
        }
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextH70(
            text = stringResource(platform.nameId),
            color = shareColors.backgroundText.copy(alpha = 0.5f),
        )
    }
}

@Preview
@Composable
private fun PlatformItemPreview(
    @PreviewParameter(SocialPlatformProvider::class) platform: SocialPlatform,
) {
    val shareColors = ShareColors(Color(0xFFEC0404))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(shareColors.background)
            .size(96.dp),
    ) {
        PlatformItem(
            platform,
            shareColors,
            onClick = {},
        )
    }
}

private class SocialPlatformProvider : PreviewParameterProvider<SocialPlatform> {
    override val values get() = SocialPlatform.entries.asSequence()
}
