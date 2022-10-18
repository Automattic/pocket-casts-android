package au.com.shiftyjelly.pocketcasts.compose.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModalBottomSheet(
    showOnLoad: Boolean = false,
    content: BottomSheetContentState.Content,
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = if (showOnLoad) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden,
    )
    val coroutineScope = rememberCoroutineScope()

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
        content = {}
    )
}

@OptIn(ExperimentalMaterialApi::class)
fun hideBottomSheet(coroutineScope: CoroutineScope, sheetState: ModalBottomSheetState) {
    coroutineScope.launch {
        sheetState.hide()
    }
}
