package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState

object RequirePlusScreen {
    const val route = "requirePlus"
}

@Composable
fun RequirePlusScreen(
    columnState: ScalingLazyColumnState,
    onContinueToLogin: () -> Unit,
) {

    val viewModel = hiltViewModel<RequirePlusViewModel>()

    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            Text(
                text = "[INSERT BEAUTIFUL UI HERE]\nPlease sign in with a Plus account to use Pocket Casts on your watch",
                modifier = Modifier.clickable { onContinueToLogin() }
            )
        }
    }
}
