package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EndOfYearPromptCard(
    onClick: () -> Unit,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageId: Int = IR.drawable.playback_banner,
    title: String = stringResource(LR.string.end_of_year_prompt_card_title),
    summary: String = stringResource(LR.string.end_of_year_prompt_card_summary),
    summaryMaxWidth: Dp = Dp.Unspecified,
) {
    CallOnce {
        onShow()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(imageId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            TextH30(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.36).sp,
            )
            TextH70(
                text = summary,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(max = summaryMaxWidth * LocalDensity.current.fontScale),
            )
        }
    }
}

@Preview
@Composable
private fun EndOfYearPromptCardPreview() {
    EndOfYearPromptCard(onClick = {}, onShow = {})
}

@Preview
@Composable
private fun Playback2026PromptCardPreview() {
    EndOfYearPromptCard(
        onClick = {},
        onShow = {},
        imageId = IR.drawable.playback_2026_banner,
        title = stringResource(LR.string.playback_2026_banner_title),
        summary = stringResource(LR.string.playback_2026_banner_description),
        summaryMaxWidth = 197.dp,
    )
}
