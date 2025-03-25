package au.com.shiftyjelly.pocketcasts.compose.bottomsheet

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    parent: ViewGroup,
    onExpanded: () -> Unit,
    shouldShow: Boolean,
    content: BottomSheetContentState.Content,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    ),
) {
    val coroutineScope = rememberCoroutineScope()
    var isSheetShown by remember { mutableStateOf(false) }

    BackHandler(sheetState.isVisible) {
        hideBottomSheet(coroutineScope, sheetState)
    }

    androidx.compose.material3.ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            hideBottomSheet(coroutineScope, sheetState)
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = Color.Black.copy(alpha = .25f),
        content = {
            BottomSheetContent(
                state = BottomSheetContentState(content),
                onDismiss = {
                    hideBottomSheet(coroutineScope, sheetState)
                },
            )
        },
    )

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.currentValue }
            .collect {
                if (sheetState.currentValue == SheetValue.Expanded) {
                    onExpanded.invoke()
                } else if (sheetState.currentValue == SheetValue.Hidden) {
                    if (isSheetShown) {
                        /* Remove bottom sheet from parent view when bottom sheet is hidden
                        on dismiss or back action for talkback to function properly. */
                        parent.removeAllViews()
                    } else {
                        if (!sheetState.isVisible && shouldShow) {
                            /* Show bottom sheet when it is hidden on initial set up */
                            displayBottomSheet(coroutineScope, sheetState)
                            isSheetShown = true
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun hideBottomSheet(coroutineScope: CoroutineScope, sheetState: SheetState) {
    coroutineScope.launch {
        sheetState.hide()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun displayBottomSheet(coroutineScope: CoroutineScope, sheetState: SheetState) {
    coroutineScope.launch {
        sheetState.show()
    }
}
