package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonProperties
import au.com.shiftyjelly.pocketcasts.compose.components.SimpleDialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun RefreshSection(
    refreshState: RefreshState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    ErrorInfoDialog(
        message = errorDialogMessage,
        onDismiss = { errorDialogMessage = null },
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (refreshState is RefreshState.Failed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { errorDialogMessage = refreshState.error },
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_alert_small),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryIcon02,
                )
                Spacer(
                    modifier = Modifier.width(8.dp),
                )
                TextP50(
                    text = refreshState.toLabel(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        } else {
            TextP50(
                text = refreshState.toLabel(),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        OutlinedButton(
            border = ButtonDefaults.outlinedBorder.copy(
                brush = SolidColor(MaterialTheme.theme.colors.primaryUi05),
                width = 2.dp,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(10.dp),
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_retry),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryText02,
            )
            Spacer(
                modifier = Modifier.width(4.dp),
            )
            TextP50(
                text = stringResource(LR.string.profile_refresh_now),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Composable
private fun RefreshState?.toLabel() = when (this) {
    is RefreshState.Failed -> stringResource(LR.string.profile_refresh_failed)
    is RefreshState.Never -> stringResource(LR.string.profile_refreshed_never)
    is RefreshState.Refreshing -> stringResource(LR.string.profile_refreshing)
    is RefreshState.Success -> {
        val resources = LocalContext.current.resources
        val timeAmount = remember(date) {
            val duration = (Date().time - date.time).milliseconds
            duration.toFriendlyString(resources, maxPartCount = 1)
        }
        stringResource(LR.string.profile_last_refresh, timeAmount)
    }

    null -> stringResource(LR.string.profile_refresh_status_unknown)
}

@Composable
private fun ErrorInfoDialog(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message != null) {
        SimpleDialog(
            onDismissRequest = onDismiss,
            title = stringResource(LR.string.profile_refresh_error),
            body = message,
            buttonProperties = listOf(
                DialogButtonProperties(
                    text = stringResource(LR.string.ok),
                    onClick = onDismiss,
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun RefreshSectionStatePreview(
    @PreviewParameter(RefreshStateParameterProvider::class) state: RefreshState?,
) {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        Box(
            modifier = Modifier.padding(32.dp),
        ) {
            RefreshSection(
                refreshState = state,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun RefreshSectionThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        Box(
            modifier = Modifier.padding(32.dp),
        ) {
            RefreshSection(
                refreshState = RefreshState.Success(Date()),
                onClick = {},
            )
        }
    }
}

private class RefreshStateParameterProvider : PreviewParameterProvider<RefreshState?> {
    override val values = sequenceOf(
        null,
        RefreshState.Failed("Oh no!"),
        RefreshState.Never,
        RefreshState.Refreshing,
        RefreshState.Success(Date.from(Instant.now().minus(2, ChronoUnit.HOURS))),
    )
}
