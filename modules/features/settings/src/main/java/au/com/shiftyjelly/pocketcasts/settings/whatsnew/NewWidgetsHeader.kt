package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val PaddingTop = 80
private const val ImageSize = 150

@Composable
fun NewWidgetsHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fullModal: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .height((ImageSize * 2 + 80).dp)
                .then(if (fullModal) Modifier else Modifier.padding(top = PaddingTop.dp)),
        ) {
            Image(
                painter = painterResource(IR.drawable.whats_new_widgets),
                contentDescription = null,
                modifier = Modifier.padding(48.dp),
            )
        }

        if (!fullModal) {
            RowCloseButton(
                onClose = onClose,
                tintColor = if (MaterialTheme.theme.isLight) Color.Black else Color.White,
            )
        }
    }
}
