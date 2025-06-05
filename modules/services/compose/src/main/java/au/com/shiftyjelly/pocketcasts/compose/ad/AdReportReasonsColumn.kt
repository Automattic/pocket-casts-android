package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.type.AdReportReason
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AdReportReasonsColumn(
    colors: AdColors.ReportSheet,
    onReportAd: (AdReportReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(colors.ripple)) {
        Column(
            modifier = modifier,
        ) {
            TextC50(
                text = stringResource(LR.string.ad_report_reason_question).uppercase(),
                color = colors.highlightText,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            AdReportReason.entries.forEachIndexed { index, reason ->
                AdReportReasonRow(
                    reason = reason,
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = { onReportAd(reason) },
                            role = Role.Button,
                        )
                        .padding(horizontal = 24.dp),
                )
                if (index != AdReportReason.entries.lastIndex) {
                    Divider(
                        color = colors.divider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdReportReasonRow(
    reason: AdReportReason,
    colors: AdColors.ReportSheet,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.heightIn(min = 74.dp),
    ) {
        TextH30(
            text = stringResource(reason.textLabel),
            color = colors.primaryText,
        )
    }
}

private val AdReportReason.textLabel get() = when (this) {
    AdReportReason.Broken -> LR.string.ad_report_reason_broken
    AdReportReason.Malicious -> LR.string.ad_report_reason_malicious
    AdReportReason.TooFrequent -> LR.string.ad_report_reason_too_often
    AdReportReason.Other -> LR.string.ad_report_reason_other
}

@Preview
@Composable
private fun AdReportReasonsColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        val colors = rememberAdColors().reportSheet
        AdReportReasonsColumn(
            colors = colors,
            onReportAd = {},
            modifier = Modifier.background(colors.surface),
        )
    }
}

@Preview
@Composable
private fun AdReportReasonsColumnPodcastPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val colors = rememberAdColors().reportSheet
            AdReportReasonsColumn(
                colors = colors,
                onReportAd = {},
                modifier = Modifier.background(colors.surface),
            )
        }
    }
}
