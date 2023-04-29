package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.localization.R
import kotlinx.coroutines.delay

@Composable
fun PasswordScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    navigateOnSignInSuccess: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val signInState by viewModel.signInState.observeAsState()
        if (signInState is SignInState.Success) {
            LaunchedEffect(Unit) {
                delay(1000)
                navigateOnSignInSuccess()
            }
        }

        val password by viewModel.password.observeAsState()
        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        val onNext = { viewModel.signIn() }
        FormField(
            value = password ?: "",
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = stringResource(R.string.profile_password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    if (passwordVisible) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.wear_hide_password),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.wear_show_password),
                        )
                    }
                }
            },
            onImeAction = onNext,
            singleLine = false,
            modifier = Modifier
                .padding(all = 8.dp)
        )

        Button(
            onClick = onNext,
            enabled = password?.isNotBlank() == true
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.profile_confirm),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center)
            )
        }

        Text(
            text = signInState.toString(),
            fontSize = 6.sp
        )
    }
}
