package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

object RequirePlusScreen {
    const val ROUTE = "requirePlus"
}

@Composable
fun RequirePlusScreen(
    onContinueToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequirePlusViewModel = hiltViewModel(),
) {
    val columnState = rememberResponsiveColumnState()

    ScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
    ) {
        CallOnce {
            viewModel.onShown()
        }

        ScalingLazyColumn(
            columnState = columnState,
        ) {
            item {
                SubscriptionBadge(
                    iconRes = IR.drawable.ic_plus,
                    shortNameRes = LR.string.pocket_casts_plus_short,
                    iconColor = Color.Black,
                    backgroundColor = colorResource(UR.color.plus_gold),
                    textColor = Color.Black,
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(LR.string.log_in_watch_requires_plus))
                        append(" ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("play.pocketcasts.com")
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onContinueToLogin() },
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                WatchListChip(
                    title = stringResource(LR.string.log_in),
                    iconRes = IR.drawable.signin,
                    onClick = onContinueToLogin,
                )
            }
        }
    }
}
