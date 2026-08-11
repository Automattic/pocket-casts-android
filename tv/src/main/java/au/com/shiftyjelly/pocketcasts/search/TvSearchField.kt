package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun TvSearchField(
    query: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.tvColors.textSecondary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(20.dp))
        if (query.isEmpty()) {
            Text(
                text = stringResource(LR.string.search),
                style = MaterialTheme.tvTypography.title2,
                color = MaterialTheme.tvColors.textSecondary,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(scrollState),
            ) {
                Text(
                    text = query,
                    style = MaterialTheme.tvTypography.title2,
                    color = MaterialTheme.tvColors.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(MaterialTheme.tvColors.textPrimary),
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchFieldPreview() {
    TvTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        ) {
            TvSearchField(query = "huberman")
        }
    }
}
