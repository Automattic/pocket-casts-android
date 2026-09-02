package au.com.shiftyjelly.pocketcasts.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
import au.com.shiftyjelly.pocketcasts.theme.TvCardShape
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvBannerRow(
    banner: TvDiscoverBanner,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    TvTile(
        onClick = onClick,
        shape = CardDefaults.shape(TvCardShape),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        colors = CardDefaults.colors(
            containerColor = Color.Black,
            focusedContainerColor = Color.Black,
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(153.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            BackgroundLift()
            Image(
                painter = painterResource(banner.artwork()),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .requiredHeight(banner.artworkHeight),
            )
            if (banner.hasArtworkMask) {
                // Opaque black over the text side so the bright collage only shows on the end edge.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black,
                                0.82f to Color.Black,
                                1f to Color.Transparent,
                            ),
                        ),
                )
                BackgroundLift()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(banner.contentWidthFraction)
                    .padding(horizontal = 36.dp),
            ) {
                BannerActionPill(banner, isFocused)
                BannerText(banner, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun BackgroundLift() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    0f to MaterialTheme.tvColors.backgroundActive20,
                    0.55f to Color.Transparent,
                ),
            ),
    )
}

@Composable
private fun BannerText(banner: TvDiscoverBanner, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.Center, modifier = modifier) {
        Text(
            text = banner.title(),
            style = MaterialTheme.tvTypography.callout,
            color = MaterialTheme.tvColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = banner.subtitle(),
            style = MaterialTheme.tvTypography.body,
            color = MaterialTheme.tvColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BannerActionPill(banner: TvDiscoverBanner, isFocused: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isFocused) MaterialTheme.tvColors.backgroundActive else MaterialTheme.tvColors.backgroundActive20)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            text = banner.actionTitle(),
            style = MaterialTheme.tvTypography.caption1,
            color = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.backgroundActive,
        )
    }
}

@DrawableRes
private fun TvDiscoverBanner.artwork(): Int = when (this) {
    TvDiscoverBanner.CreateAccount -> IR.drawable.tv_banner_create_account
    TvDiscoverBanner.DiscoverMore -> IR.drawable.tv_banner_discover_more
}

private val TvDiscoverBanner.artworkHeight: Dp
    get() = when (this) {
        TvDiscoverBanner.CreateAccount -> 174.dp
        TvDiscoverBanner.DiscoverMore -> 197.dp
    }

private val TvDiscoverBanner.contentWidthFraction: Float
    get() = when (this) {
        TvDiscoverBanner.CreateAccount -> 0.68f
        TvDiscoverBanner.DiscoverMore -> 0.82f
    }

private val TvDiscoverBanner.hasArtworkMask: Boolean
    get() = this == TvDiscoverBanner.DiscoverMore

@Composable
private fun TvDiscoverBanner.title(): String = when (this) {
    TvDiscoverBanner.CreateAccount -> stringResource(LR.string.tv_banner_create_account_title)
    TvDiscoverBanner.DiscoverMore -> stringResource(LR.string.tv_banner_discover_more_title)
}

@Composable
private fun TvDiscoverBanner.subtitle(): String = when (this) {
    TvDiscoverBanner.CreateAccount -> stringResource(LR.string.tv_banner_create_account_subtitle)
    TvDiscoverBanner.DiscoverMore -> stringResource(LR.string.tv_banner_discover_more_subtitle)
}

@Composable
private fun TvDiscoverBanner.actionTitle(): String = when (this) {
    TvDiscoverBanner.CreateAccount -> stringResource(LR.string.tv_banner_create_account_action_title)
    TvDiscoverBanner.DiscoverMore -> stringResource(LR.string.tv_banner_discover_more_action_title)
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvBannerRowPreview() {
    TvTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(36.dp),
        ) {
            TvBannerRow(banner = TvDiscoverBanner.CreateAccount, onClick = {})
            TvBannerRow(banner = TvDiscoverBanner.DiscoverMore, onClick = {})
        }
    }
}
