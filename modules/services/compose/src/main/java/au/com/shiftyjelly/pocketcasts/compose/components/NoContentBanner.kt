package au.com.shiftyjelly.pocketcasts.compose.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NoContentBanner(
    title: String,
    body: String,
    @DrawableRes iconResourceId: Int,
    modifier: Modifier = Modifier,
    primaryButtonText: String? = null,
    onPrimaryButtonClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = Util.isTablet(LocalContext.current)
    val isPortraitOrTablet = isTablet || !isLandscape
    val itemSpaceHeight = if (isPortraitOrTablet) 16.dp else 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpaceHeight),
        modifier = modifier
            .padding(horizontal = 32.dp)
            .widthIn(max = if (isTablet || isLandscape) 450.dp else 330.dp),
    ) {
        if (isPortraitOrTablet) {
            Image(
                painter = painterResource(id = iconResourceId),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            )
        }

        TextH30(
            text = title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500,
        )

        if (body.isNotEmpty()) {
            TextP40(
                text = body,
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText02,
                fontSize = 15.sp,
                fontWeight = FontWeight.W400,
            )
        }

        EmptyStateButtons(
            primaryButtonText = primaryButtonText,
            onPrimaryButtonClick = onPrimaryButtonClick,
            secondaryButtonText = secondaryButtonText,
            onSecondaryButtonClick = onSecondaryButtonClick,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EmptyStateButtons(
    primaryButtonText: String? = null,
    onPrimaryButtonClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
) {
    if (primaryButtonText != null || secondaryButtonText != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (primaryButtonText != null) {
                CompositionLocalProvider(
                    LocalRippleConfiguration provides RippleConfiguration(MaterialTheme.theme.colors.primaryUi01),
                ) {
                    RowButton(
                        text = primaryButtonText,
                        onClick = { onPrimaryButtonClick?.invoke() },
                        includePadding = false,
                        textColor = MaterialTheme.theme.colors.primaryInteractive02,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                        ),
                    )
                }
            }

            if (secondaryButtonText != null) {
                val rippleColors = MaterialTheme.theme.colors.primaryInteractive01
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            indication = remember(rippleColors) { ripple(color = rippleColors) },
                            interactionSource = null,
                            role = Role.Button,
                            onClick = { onSecondaryButtonClick?.invoke() },
                        )
                        .padding(horizontal = 16.dp),
                ) {
                    TextP40(
                        text = secondaryButtonText,
                        fontSize = 17.sp,
                        color = MaterialTheme.theme.colors.primaryInteractive01,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun UpNextNoContentBannerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType = themeType) {
        NoContentBanner(
            title = "Time to add some podcasts",
            body = "Discover and subscribe to your favorite podcasts.",
            iconResourceId = IR.drawable.ic_podcasts,
            primaryButtonText = "Discover",
            secondaryButtonText = "How do I do that?",
        )
    }
}

@Preview
@Composable
private fun UpNextNoContentBannerWithoutSubtitlePreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        NoContentBanner(
            title = "Time to add some podcasts",
            body = "",
            iconResourceId = IR.drawable.ic_podcasts,
            primaryButtonText = "Discover",
            secondaryButtonText = "How do I do that?",
        )
    }
}

@Preview
@Composable
private fun UpNextNoContentBannerWithoutPrimaryButtonPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        NoContentBanner(
            title = "Time to add some podcasts",
            body = "Discover and subscribe to your favorite podcasts.",
            iconResourceId = IR.drawable.ic_podcasts,
            secondaryButtonText = "How do I do that?",
        )
    }
}

@Preview
@Composable
private fun UpNextNoContentBannerWithoutSecondaryButtonPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        NoContentBanner(
            title = "Time to add some podcasts",
            body = "Discover and subscribe to your favorite podcasts.",
            iconResourceId = IR.drawable.ic_podcasts,
            primaryButtonText = "Discover",
        )
    }
}
