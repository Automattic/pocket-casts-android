package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ErrorScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingSpinner
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginWithEmailScreen(
    onSignInSuccess: () -> Unit,
) {
    val viewModel = hiltViewModel<SignInViewModel>()
    val signInState by viewModel.signInState.observeAsState()
    val email by viewModel.email.observeAsState()
    val password by viewModel.password.observeAsState()

    var loading by remember { mutableStateOf(false) }

    when (signInState) {
        null,
        SignInState.Empty,
        -> {
            loading = false

            if (email.isNullOrBlank()) {
                val label = stringResource(LR.string.enter_email)
                val launcher = getLauncher { viewModel.updateEmail(it) }
                LaunchedEffect(Unit) {
                    launchRemoteInput(label, launcher)
                }
            } else if (password.isNullOrEmpty()) {
                val label = stringResource(LR.string.enter_password)
                val launcher = getLauncher {
                    viewModel.updatePassword(it)
                }
                LaunchedEffect(Unit) {
                    launchRemoteInput(label, launcher)
                }
            } else {
                viewModel.signIn()
            }
        }

        SignInState.Loading -> {
            loading = true
        }

        is SignInState.Success -> {
            LaunchedEffect(Unit) {
                onSignInSuccess()
            }
        }

        is SignInState.Failure -> {
            loading = false
            val currentState = signInState as? SignInState.Failure
            val message = currentState?.message
                ?: stringResource(
                    currentState?.errors?.last()?.message
                        ?: LR.string.error_login_failed,
                )
            ErrorScreen(message)
        }
    }

    if (loading) {
        Loading()
    }
}

@Composable
private fun Loading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadingSpinner(Modifier.size(36.dp))
    }
}

private const val key = "key"

@Composable
private fun getLauncher(onResult: (String) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it.data?.let { data ->
            val results: Bundle = RemoteInput.getResultsFromIntent(data)
            results.getCharSequence(key)?.let { chars ->
                onResult(chars.toString())
            }
        }
    }

private fun launchRemoteInput(
    label: String,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    val remoteInputs: List<RemoteInput> = listOf(
        RemoteInput.Builder(key)
            .setLabel(label)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build(),
    )

    val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

    launcher.launch(intent)
}
