package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.localization.R

@Composable
fun LoginWithEmailScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    listState: ScalingLazyListState,
    navigateToPasswordScreen: () -> Unit,
) {

    val email by viewModel.email.observeAsState()

    ScalingLazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {

        item {
            FormField(
                value = email ?: "",
                onValueChange = { viewModel.updateEmail(it) },
                placeholder = "",
                onImeAction = navigateToPasswordScreen,
                singleLine = false,
                modifier = Modifier
                    .padding(all = 8.dp)
                    .padding(top = 16.dp)
            )
        }

        item {
            Button(
                onClick = navigateToPasswordScreen,
                enabled = email?.isNotBlank() == true
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.profile_confirm),
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        }
    }
}
