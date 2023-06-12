package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.BuildConfig
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object WearAboutScreen {
    const val route = "wear_about_screen"
}

@Composable
fun WearAboutScreen(columnState: ScalingLazyColumnState) {
    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            Image(
                painter = painterResource(IR.drawable.about_logo_pocketcasts),
                contentDescription = stringResource(LR.string.settings_app_icon),
            )
        }

        item {
            Text(
                text = stringResource(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // share
        // website
        // instagram
        // twitter

//        item {
//            WatchListChip(
//                title = stringResource(LR.string.settings_about_terms_of_serivce),
//                onClick = { TODO() }
//            )
//        }
//
//        item {
//            WatchListChip(
//                title = stringResource(LR.string.settings_about_privacy_policy),
//                onClick = { TODO() }
//            )
//        }
//
//        item {
//            WatchListChip(
//                title = stringResource(LR.string.settings_about_acknowledgements),
//                onClick = { TODO() }
//            )
//        }
    }
}
