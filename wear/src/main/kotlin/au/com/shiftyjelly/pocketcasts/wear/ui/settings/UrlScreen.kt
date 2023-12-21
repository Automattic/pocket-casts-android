package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.material.Text
import androidx.wear.remote.interactions.RemoteActivityHelper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.navscaffold.scrollable
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UrlScreenRoutes {
    const val termsOfService = "url_screen_terms_of_service"
    const val privacy = "url_screen_privacy_policy"
}

fun NavGraphBuilder.settingsUrlScreens() {
    scrollable(UrlScreenRoutes.termsOfService) {
        UrlScreen(
            title = stringResource(LR.string.settings_about_terms_of_serivce),
            message = stringResource(LR.string.settings_about_terms_of_service_available_at, Settings.INFO_TOS_URL),
            url = Settings.INFO_TOS_URL,
            columnState = it.columnState,
        )
    }

    scrollable(UrlScreenRoutes.privacy) {
        UrlScreen(
            title = stringResource(id = LR.string.settings_about_privacy_policy),
            message = stringResource(LR.string.settings_about_privacy_policy_available_at, Settings.INFO_PRIVACY_URL),
            url = Settings.INFO_PRIVACY_URL,
            columnState = it.columnState,
        )
    }
}

@Composable
fun UrlScreen(
    title: String,
    message: String,
    url: String,
    columnState: ScalingLazyColumnState,
) {

    val coroutineScope = rememberCoroutineScope()

    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            ScreenHeaderChip(text = title)
        }

        item {
            Text(
                text = message,
                textAlign = TextAlign.Center,
            )
        }

        item {
            val context = LocalContext.current
            WatchListChip(
                title = stringResource(LR.string.settings_open_on_phone),
                onClick = {
                    coroutineScope.launch {
                        openUrlOnPhone(url, context)
                    }
                }
            )
        }
    }
}

private suspend fun openUrlOnPhone(url: String, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(url))
        RemoteActivityHelper(context, Executors.newSingleThreadExecutor())
            .startRemoteActivity(intent)
            .await()
    } catch (e: Exception) {
        Timber.i("UrlScreen failed to open url $url on phone")
        Toast.makeText(context, LR.string.settings_could_not_open_on_phone, Toast.LENGTH_SHORT)
            .show()
    }
}
