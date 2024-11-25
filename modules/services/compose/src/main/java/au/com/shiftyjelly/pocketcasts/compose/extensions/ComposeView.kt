package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

fun ComposeView.setContentWithViewCompositionStrategy(
    viewCompositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    content: @Composable (() -> Unit),
) {
    setViewCompositionStrategy(viewCompositionStrategy)
    setContent { content() }
}
