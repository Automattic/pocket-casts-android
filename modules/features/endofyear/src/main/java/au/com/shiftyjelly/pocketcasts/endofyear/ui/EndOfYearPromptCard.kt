package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EndOfYearPromptCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = Color(0xFF27486A))
            .clickable { onClick.invoke() },
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(IR.drawable.playback_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
                .fillMaxWidth(0.4f),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                TextH30(
                    text = stringResource(LR.string.end_of_year_prompt_card_title),
                    color = Color.White,
                    disableAutoScale = true,
                )
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
                TextH70(
                    text = stringResource(LR.string.end_of_year_prompt_card_summary),
                    color = Color.White,
                    fontWeight = FontWeight.W600,
                    disableAutoScale = true,
                )
            }
        }
    }
}

@Preview
@Composable
private fun EndOfYearPromptCardPreview() {
    EndOfYearPromptCard(onClick = {})
}
