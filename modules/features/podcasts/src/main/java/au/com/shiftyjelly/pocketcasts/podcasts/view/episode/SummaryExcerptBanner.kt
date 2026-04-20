package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SummaryExcerptBanner(
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .shadow(elevation = 2.dp, shape = BannerShape)
            .background(MaterialTheme.theme.colors.primaryUi04, BannerShape)
            .then(modifier)
            .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_ai),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(LR.string.view_summary),
            color = MaterialTheme.theme.colors.primaryText01,
            fontSize = 16.sp,
            lineHeight = 16.sp * 1.5f,
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(IR.drawable.ic_chevron_right),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val BannerShape = RoundedCornerShape(8.dp)
