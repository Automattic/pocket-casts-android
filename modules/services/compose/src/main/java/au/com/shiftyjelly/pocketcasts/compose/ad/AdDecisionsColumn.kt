package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AdDecisionsColumn(
    colors: AdColors.ReportSheet,
    onClickRemoveAds: () -> Unit,
    onClickReportAd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(colors.ripple)) {
        Column(
            modifier = modifier,
        ) {
            AdDecisionRow(
                iconId = IR.drawable.ic_remove_ads,
                text = stringResource(LR.string.remove_ads),
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onClickRemoveAds,
                        role = Role.Button,
                    )
                    .padding(horizontal = 24.dp),
            )
            Divider(
                color = colors.divider,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            AdDecisionRow(
                iconId = IR.drawable.ic_report_ad,
                text = stringResource(LR.string.report_ad),
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onClickReportAd,
                        role = Role.Button,
                    )
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun AdDecisionRow(
    @DrawableRes iconId: Int,
    text: String,
    colors: AdColors.ReportSheet,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.heightIn(min = 74.dp),
    ) {
        Image(
            painter = painterResource(iconId),
            colorFilter = ColorFilter.tint(colors.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        TextH30(
            text = text,
            color = colors.primaryText,
        )
    }
}

@Preview
@Composable
private fun AdDecisionsColumnThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        val colors = rememberAdColors().reportSheet
        AdDecisionsColumn(
            colors = colors,
            onClickRemoveAds = {},
            onClickReportAd = {},
            modifier = Modifier.background(colors.surface),
        )
    }
}

@Preview
@Composable
private fun AdDecisionsColumnPodcastPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val colors = rememberAdColors().reportSheet
            AdDecisionsColumn(
                colors = colors,
                onClickRemoveAds = {},
                onClickReportAd = {},
                modifier = Modifier.background(colors.surface),
            )
        }
    }
}
