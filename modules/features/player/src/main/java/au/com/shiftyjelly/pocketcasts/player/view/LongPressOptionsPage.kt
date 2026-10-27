package au.com.shiftyjelly.pocketcasts.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LongPressOptionsPage(
    showNextEpisode: Boolean,
    onMarkPlayedClick: () -> Unit,
    onNextEpisodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .background(MaterialTheme.theme.colors.playerContrast01, RoundedCornerShape(3.dp)),
            )
        }
        OptionRow(
            label = stringResource(LR.string.mark_played),
            onClick = onMarkPlayedClick,
        )
        if (showNextEpisode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.theme.colors.playerContrast05),
            )
            OptionRow(
                label = stringResource(LR.string.next_episode),
                onClick = onNextEpisodeClick,
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides OptionRippleConfiguration) {
        Box(
            contentAlignment = TextViewAlignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = label }
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                ),
        ) {
            TextViewText(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.theme.colors.playerContrast01,
                modifier = Modifier
                    .padding(start = 32.dp)
                    .clearAndSetSemantics {},
            )
        }
    }
}

private val OptionRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(draggedAlpha = 0.2f, focusedAlpha = 0.2f, hoveredAlpha = 0.2f, pressedAlpha = 0.2f),
)

@Preview
@Composable
private fun LongPressOptionsPagePreview() {
    AppTheme(Theme.ThemeType.DARK) {
        LongPressOptionsPage(
            showNextEpisode = true,
            onMarkPlayedClick = {},
            onNextEpisodeClick = {},
        )
    }
}
