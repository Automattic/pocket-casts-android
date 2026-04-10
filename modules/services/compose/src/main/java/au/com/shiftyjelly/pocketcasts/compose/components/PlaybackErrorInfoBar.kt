package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.theme

/**
 * Fullscreen player variant uses player colors.
 */
@Composable
fun PlaybackErrorInfoBar(
    message: String,
    playerColors: PlayerColors,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    linkText: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
    ) {
        InfoBarText(
            message = message,
            linkText = linkText,
            color = playerColors.contrast01,
            linkColor = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * Miniplayer variant uses theme colors.
 */
@Composable
fun PlaybackErrorInfoBar(
    message: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    linkText: String? = null,
) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.primaryUi03)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 28.dp),
    ) {
        InfoBarText(
            message = message,
            linkText = linkText,
            color = colors.primaryText01,
            linkColor = colors.primaryInteractive01,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun InfoBarText(
    message: String,
    linkText: String?,
    color: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
) {
    if (linkText != null) {
        val annotatedString = buildAnnotatedString {
            append(message)
            append(" ")
            withStyle(SpanStyle(color = linkColor)) {
                append(linkText)
            }
        }
        Text(
            text = annotatedString,
            color = color,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    } else {
        TextH50(
            text = message,
            color = color,
            disableAutoScale = true,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}
