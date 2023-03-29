package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ObtainConfirmationScreen(
    text: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize()
    ) {
        TextH30(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {

            Button(
                onClick = onCancel,
                modifier = Modifier.size(52.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(color = Color(0xFF202124)) // FIXME
                        .clip(CircleShape)
                        .fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_close),
                        tint = Color.White,
                        contentDescription = stringResource(LR.string.cancel)
                    )
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.size(52.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(color = Color.Red)
                        .clip(CircleShape)
                        .fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_check_black_24dp),
                        tint = Color.Black,
                        contentDescription = stringResource(LR.string.profile_confirm)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ObtainConfirmationScreen(
        text = "Are you sure?",
        onConfirm = {},
        onCancel = {},
    )
}
