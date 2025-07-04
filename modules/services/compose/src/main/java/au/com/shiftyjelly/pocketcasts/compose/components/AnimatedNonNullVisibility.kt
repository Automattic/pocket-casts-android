package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun <T : Any> AnimatedNonNullVisibility(
    item: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = defaultEnterTransition,
    exit: ExitTransition = defaultExitTransition,
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) {
    var lastNonNull by remember { mutableStateOf<T?>(item) }
    if (item != null) {
        lastNonNull = item
    }
    AnimatedVisibility(
        visible = item != null,
        enter = enter,
        exit = exit,
        label = label,
        modifier = modifier,
    ) {
        lastNonNull?.let { nonNullItem ->
            content(nonNullItem)
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
private val expandVertically = expandVertically()
private val shrinkVertically = shrinkVertically()

private val defaultEnterTransition = fadeIn + expandVertically
private val defaultExitTransition = fadeOut + shrinkVertically
