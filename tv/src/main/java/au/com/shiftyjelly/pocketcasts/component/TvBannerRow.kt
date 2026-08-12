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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
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
    val containerColor = if (isFocused) MaterialTheme.tvColors.backgroundOverlay else MaterialTheme.tvColors.backgroundSurface

    TvTile(
        onClick = onClick,
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundSurface,
            focusedContainerColor = MaterialTheme.tvColors.backgroundOverlay,
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            Image(
                painter = painterResource(banner.artwork()),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .requiredHeight(BannerArtworkHeight),
            )
            if (banner.hasGradient) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to containerColor,
                                0.82f to containerColor,
                                1f to containerColor.copy(alpha = 0f),
                            ),
                        ),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (banner.hasGradient) 0.82f else 0.68f)
                    .padding(horizontal = 48.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isFocused) MaterialTheme.tvColors.backgroundActive else MaterialTheme.tvColors.backgroundActive20)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = banner.actionTitle(),
                        style = MaterialTheme.tvTypography.caption1,
                        color = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.backgroundActive,
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
                Column(verticalArrangement = Arrangement.Center) {
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
        }
    }
}

private val BannerArtworkHeight = 240.dp

@DrawableRes
private fun TvDiscoverBanner.artwork(): Int = when (this) {
    TvDiscoverBanner.CreateAccount -> IR.drawable.tv_banner_create_account
    TvDiscoverBanner.DiscoverMore -> IR.drawable.tv_banner_discover_more
}

private val TvDiscoverBanner.hasGradient: Boolean
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        ) {
            TvBannerRow(banner = TvDiscoverBanner.CreateAccount, onClick = {})
            TvBannerRow(banner = TvDiscoverBanner.DiscoverMore, onClick = {})
        }
    }
}
