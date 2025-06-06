package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

data class TimePlayButtonColors(
    val text: Color,
    val border: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = TimePlayButtonColors(
            text = colors.primaryText01,
            border = colors.primaryText01,
        )

        fun player(colors: PlayerColors) = TimePlayButtonColors(
            text = colors.contrast01,
            border = colors.contrast01,
        )
    }
}

@Composable
fun TimePlayButton(
    timeSecs: Int,
    @StringRes contentDescriptionId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: TimePlayButtonColors = TimePlayButtonColors.default(MaterialTheme.theme.colors),
) {
    val timeText = TimeHelper.formattedSeconds(timeSecs.toDouble())
    val description = stringResource(contentDescriptionId, timeText)

    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
        ),
        shape = CircleShape,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        TextH40(
            text = timeText,
            color = colors.text,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            painter = painterResource(IR.drawable.ic_play),
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier.size(10.dp, 13.dp),
        )
    }
}

@Preview
@Composable
private fun TimePlayButtonPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        TimePlayButton(
            timeSecs = 121,
            contentDescriptionId = R.string.bookmark_play,
            onClick = {},
        )
    }
}
