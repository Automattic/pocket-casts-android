package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TranscriptExcerptBanner(
    text: String,
    isGenerated: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .widthIn(max = 480.dp)
            .shadow(elevation = 2.dp, shape = BackgroundShape)
            .background(MaterialTheme.theme.colors.primaryUi04, BackgroundShape)
            .then(modifier)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextH40(
                text = stringResource(LR.string.transcript),
            )
            if (isGenerated) {
                Image(
                    painter = painterResource(IR.drawable.ic_ai),
                    colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = text,
            color = MaterialTheme.theme.colors.primaryText01,
            fontSize = 14.sp,
            lineHeight = 14.sp * 1.5f,
            fontFamily = TranscriptTheme.RobotoSerifFontFamily,
        )
    }
}

private val BackgroundShape = RoundedCornerShape(8.dp)

@Preview
@Composable
private fun TranscriptExcerptBannerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TranscriptExcerptBanner(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim…",
                isGenerated = false,
            )
            TranscriptExcerptBanner(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim…",
                isGenerated = true,
            )
        }
    }
}
