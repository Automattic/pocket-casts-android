package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ShuffleAnimation(modifier: Modifier = Modifier) {

}

private val dateFormatter = DateTimeFormatter.ISO_DATE.withZone(ZoneId.systemDefault())

private data class ShuffleConfig(
    @DrawableRes val artworkResId: Int,
    val date: Instant,
    val title: String,
    val duration: String,
)

private val predefinedShuffle = listOf(
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_0,
        date = Instant.now().minus(42, ChronoUnit.DAYS),
        title = "The Sunday Read",
        duration = "42 mins"
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_1,
        date = Instant.now().minus(2, ChronoUnit.DAYS),
        title = "Jason played the Switch 2",
        duration = "1h 22m"
    ),
    ShuffleConfig(
        artworkResId = IR.drawable.artwork_2,
        date = Instant.now().minus(12, ChronoUnit.DAYS),
        title = "EP01: Jane Doe",
        duration = "52 mins"
    ),
)

@Composable
private fun ShuffleItem(
    config: ShuffleConfig,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.theme.colors.primaryUi03,
        shape = RoundedCornerShape(3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(3.dp)),
                painter = painterResource(config.artworkResId),
                contentDescription = ""
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                TextH70(
                    fontSize = 10.sp,
                    text = dateFormatter.format(config.date),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.fillMaxWidth(),
                    disableAutoScale = true,
                )
                TextP60(
                    text = config.title,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText01,
                    disableAutoScale = true,
                )
                TextH70(
                    fontSize = 10.sp,
                    text = config.duration,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.fillMaxWidth(),
                    disableAutoScale = true,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewShuffle(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    ShuffleItem(
        config = ShuffleConfig(
            artworkResId = IR.drawable.artwork_7,
            date = Instant.now().minus(42, ChronoUnit.DAYS),
            title = "The Sunday Read",
            duration = "32 mins"
        )
    )
}