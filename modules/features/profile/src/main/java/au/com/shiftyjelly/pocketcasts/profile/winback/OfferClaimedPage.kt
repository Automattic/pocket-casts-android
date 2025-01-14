package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun OfferClaimedPage(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFCA9CA9))
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Offer Claimed",
                color = Color.Black,
            )
        }
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .background(Color(0xFFCC5079))
                .clickable(onClick = onConfirm),
        ) {
            Text(
                text = "Done",
                color = Color.White,
            )
        }
    }
}
