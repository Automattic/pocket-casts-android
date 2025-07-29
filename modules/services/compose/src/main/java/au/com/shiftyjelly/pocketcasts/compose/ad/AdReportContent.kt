package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.models.type.AdReportReason
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
fun AdReportContent(
    colors: AdColors.ReportSheet,
    onClickRemoveAds: () -> Unit,
    onReportAd: (AdReportReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isInReportMode by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(color = colors.surface),
    ) {
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Box(
            modifier = Modifier
                .size(56.dp, 4.dp)
                .background(colors.divider, CircleShape),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        AnimatedContent(
            targetState = isInReportMode,
        ) { isReport ->
            if (isReport) {
                AdReportReasonsColumn(
                    colors = colors,
                    onReportAd = onReportAd,
                )
            } else {
                AdDecisionsColumn(
                    colors = colors,
                    onClickRemoveAds = onClickRemoveAds,
                    onClickReportAd = { isInReportMode = true },
                )
            }
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AdReportContentPreview() {
    AppTheme(ThemeType.LIGHT) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
        ) {
            Spacer(
                modifier = Modifier.weight(1f),
            )
            AdReportContent(
                colors = rememberAdColors().reportSheet,
                onClickRemoveAds = {},
                onReportAd = {},
            )
        }
    }
}
