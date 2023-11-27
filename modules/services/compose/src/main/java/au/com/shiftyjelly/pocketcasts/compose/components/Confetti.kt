package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun Confetti(
    onConfettiShown: () -> Unit,
) {
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(IR.raw.confetti))
    val progress by animateLottieCompositionAsState(lottieComposition)

    LottieAnimation(
        composition = lottieComposition,
        contentScale = ContentScale.Crop,
    )
    if (progress == 1.0f) onConfettiShown()
}
