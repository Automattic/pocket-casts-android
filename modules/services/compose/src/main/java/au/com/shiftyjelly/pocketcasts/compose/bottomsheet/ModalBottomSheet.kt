package au.com.shiftyjelly.pocketcasts.compose.bottomsheet

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModalBottomSheet(
    parent: ViewGroup,
    onExpanded: () -> Unit,
    shouldShow: Boolean,
    content: BottomSheetContentState.Content,
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()
    var isSheetShown by remember { mutableStateOf(false) }

    BackHandler(sheetState.isVisible) {
        hideBottomSheet(coroutineScope, sheetState)
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            BottomSheetContent(
                state = BottomSheetContentState(content),
                onDismiss = {
                    hideBottomSheet(coroutineScope, sheetState)
                }
            )
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = Color.Black.copy(alpha = .25f),
        content = {}
    )

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.currentValue }
            .collect {
                if (sheetState.currentValue == ModalBottomSheetValue.Expanded) {
                    onExpanded.invoke()
                } else if (sheetState.currentValue == ModalBottomSheetValue.Hidden) {
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

@OptIn(ExperimentalMaterialApi::class)
private fun hideBottomSheet(coroutineScope: CoroutineScope, sheetState: ModalBottomSheetState) {
    coroutineScope.launch {
        sheetState.hide()
    }
}

@OptIn(ExperimentalMaterialApi::class)
fun displayBottomSheet(coroutineScope: CoroutineScope, sheetState: ModalBottomSheetState) {
    coroutineScope.launch {
        sheetState.show()
    }
}
