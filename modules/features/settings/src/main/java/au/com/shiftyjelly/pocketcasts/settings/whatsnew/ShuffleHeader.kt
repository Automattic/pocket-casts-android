package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShuffleHeader(modifier: Modifier = Modifier) {
    var alpha by remember { mutableFloatStateOf(0f) }

    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
    )

    LaunchedEffect(Unit) {
        delay(500)
        alpha = 1f
    }

    Icon(
        painter = painterResource(id = IR.drawable.shuffle),
        contentDescription = stringResource(LR.string.up_next_shuffle_button_content_description),
        tint = MaterialTheme.colors.primary.copy(alpha = animatedAlpha),
        modifier = modifier.size(120.dp),
    )
}
