package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ImageWidth = 361f
private val ImageHeight = 110f
private val AspectRatio = ImageWidth / ImageHeight
private val MaxImageWidth = 400.dp

@Composable
fun EndOfYearPromptCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick.invoke() }
            .widthIn(max = MaxImageWidth)
            .aspectRatio(ratio = AspectRatio),
    ) {
        val textColor = when (MaterialTheme.theme.type) {
            Theme.ThemeType.RADIOACTIVE -> MaterialTheme.theme.colors.primaryText01
            else -> Color.White
        }

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(IR.drawable.playback_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = MaterialTheme.theme.imageColorFilter,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp),

        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                TextH30(
                    text = stringResource(LR.string.end_of_year_prompt_card_title),
                    color = textColor,
                    disableAutoScale = true,
                    modifier = Modifier.fillMaxWidth(0.7f),
                )
                Spacer(
                    modifier = Modifier.height(12.dp),
                )
                TextH70(
                    text = stringResource(LR.string.end_of_year_prompt_card_summary),
                    color = textColor,
                    fontWeight = FontWeight.W600,
                    disableAutoScale = true,
                    modifier = Modifier.fillMaxWidth(0.5f),
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
