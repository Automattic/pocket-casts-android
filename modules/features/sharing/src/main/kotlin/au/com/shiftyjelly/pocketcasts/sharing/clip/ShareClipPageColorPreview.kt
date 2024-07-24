package au.com.shiftyjelly.pocketcasts.sharing.clip

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

private class PodcastColorProvider : PreviewParameterProvider<Long> {
    override val values = sequenceOf(
        "EC0404",
        "0A8654",
        "1D1A62",
        "0477C2",
        "FBCB04",
        "643491",
        "F86C7D",
        "A9CD88",
        "48448C",
        "3F7EA8",
        "D8B78F",
        "976DBF",
        "31394E",
        "56381D",
        "1B1913",
        "444641",
        "000000",
        "FFFFFF",
    ).map { Color.parseColor("#$it").toLong() }
}

@Preview(device = PreviewDevicePortrait)
@Composable
private fun VerticalCardPreview(
    @PreviewParameter(PodcastColorProvider::class) color: Long,
) = ShareClipPagePreview(color)
