package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.CoilImage
import au.com.shiftyjelly.pocketcasts.compose.extensions.fractionedSp
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun AdBanner(
    ad: BlazeAd,
    colors: AdColors.Banner,
    onAdClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 400.dp,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(colors.ripple),
    ) {
        Box(
            modifier = modifier.widthIn(max = maxWidth),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val contentDescription = stringResource(LR.string.go_to_ad, ad.text)

            Row(
                modifier = Modifier
                    .clip(AdBannerShape)
                    .background(colors.background, AdBannerShape)
                    .border(1.dp, colors.border, AdBannerShape)
                    .clickable(
                        role = Role.Button,
                        onClick = onAdClick,
                    )
                    .padding(ContentPadding)
                    .height(IntrinsicSize.Min)
                    .clearAndSetSemantics { this.contentDescription = contentDescription },

            ) {
                val context = LocalContext.current
                val theme = MaterialTheme.theme
                val isPreview = LocalInspectionMode.current
                CoilImage(
                    imageRequest = remember(context, theme.isDark, ad.imageUrl) {
                        val placeholder = if (isPreview) PlaceholderType.Small else PlaceholderType.None
                        PocketCastsImageRequestFactory(context, placeholderType = placeholder).createForFileOrUrl(ad.imageUrl)
                    },
                    title = stringResource(LR.string.ad_image),
                    showTitle = false,
                    corners = 4.dp,
                    modifier = Modifier.size(86.dp),
                    expandContentSize = true,
                )

                Spacer(
                    modifier = Modifier.width(16.dp),
                )

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        text = ad.text,
                        color = colors.ctaLabel,
                        fontSize = 14f.fractionedSp(fraction = 0.4f),
                        lineHeight = 18f.fractionedSp(fraction = 0.4f),
                        letterSpacing = 0.sp,
                    )

                    AdTitle(
                        ad = ad,
                        colors = colors,
                    )
                }

                Image(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.icon),
                    modifier = Modifier
                        .size(IconSize)
                        .indication(interactionSource, remember { ripple(radius = IconSize * 0.75f, bounded = false) })
                        .semantics { hideFromAccessibility() },
                )
            }
            val horizontalOffset = -ContentPadding.calculateRightPadding(LocalLayoutDirection.current)
            val verticalOffset = ContentPadding.calculateTopPadding()
            OverflowMenuInteractionBox(
                interactionSource = interactionSource,
                onClick = onOptionsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = horizontalOffset + (TouchTargetSize - IconSize) / 2,
                        y = verticalOffset - (TouchTargetSize - IconSize) / 2,
                    ),
            )
        }
    }
}

@Composable
private fun AdTitle(
    ad: BlazeAd,
    colors: AdColors.Banner,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(LR.string.ad).uppercase(),
            color = colors.adLabel,
            fontSize = 8f.fractionedSp(fraction = 0.4f),
            lineHeight = 8f.fractionedSp(fraction = 0.4f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            modifier = Modifier
                .background(colors.adLabelBackground, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )

        Spacer(
            modifier = Modifier.width(4.dp),
        )

        Text(
            text = ad.urlTitle,
            color = colors.titleLabel,
            fontSize = 12f.fractionedSp(fraction = 0.4f),
            lineHeight = 12f.fractionedSp(fraction = 0.4f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun OverflowMenuInteractionBox(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TouchTargetSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(LR.string.ad_options),
            ),
    )
}

private val AdBannerShape = RoundedCornerShape(8.dp)
private val ContentPadding = PaddingValues(8.dp)
private val IconSize = 24.dp
private val TouchTargetSize = 48.dp

@Preview
@Composable
private fun AdBannerThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        AdBanner(
            ad = BlazeAdPreview,
            colors = rememberAdColors().bannerAd,
            onAdClick = {},
            onOptionsClick = {},
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi01)
                .padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AdBannerPlayerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        CompositionLocalProvider(LocalPodcastColors provides PodcastColors.TheDailyPreview) {
            AdBanner(
                ad = BlazeAdPreview,
                colors = rememberAdColors().bannerAd,
                onAdClick = {},
                onOptionsClick = {},
                modifier = Modifier
                    .background(MaterialTheme.theme.rememberPlayerColors()!!.background01)
                    .padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun AdBannerPodcastPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            AdBanner(
                ad = BlazeAdPreview,
                colors = rememberAdColors().bannerAd,
                onAdClick = {},
                onOptionsClick = {},
                modifier = Modifier
                    .background(MaterialTheme.theme.rememberPlayerColors()!!.background01)
                    .padding(16.dp),
            )
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun AdBannerFontSizePreview() {
    AppTheme(ThemeType.DARK) {
        AdBanner(
            ad = BlazeAdPreview,
            colors = rememberAdColors().bannerAd,
            onAdClick = {},
            onOptionsClick = {},
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi01)
                .padding(16.dp),
        )
    }
}

private val BlazeAdPreview = BlazeAd(
    id = "",
    text = "Empower People to Discover, Share and Grow their Passion for Podcasts.",
    imageUrl = "",
    urlTitle = "pocketcasts.com",
    url = "https://pocketcasts.com",
    location = BlazeAdLocation.PodcastList,
)
