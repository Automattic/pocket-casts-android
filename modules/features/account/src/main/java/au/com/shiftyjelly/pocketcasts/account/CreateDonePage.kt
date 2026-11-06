package au.com.shiftyjelly.pocketcasts.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class CreateDonePageState(
    val isCar: Boolean,
    @DrawableRes val imageRes: Int?,
    val title: String,
    val detail: String,
    val isNewsletterChecked: Boolean,
    val isProgressVisible: Boolean,
)

@Composable
internal fun CreateDonePage(
    state: CreateDonePageState,
    onNewsletterChange: (Boolean) -> Unit,
    onCloseClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = modifier
        .fillMaxSize()
        .background(MaterialTheme.theme.colors.primaryUi01)
    if (state.isCar) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = background.verticalScroll(rememberScrollState()),
        ) {
            DoneHeader(state)
            NewsletterRow(state, onNewsletterChange, Modifier.padding(top = 32.dp).width(560.dp))
            RowButton(
                text = stringResource(LR.string.done),
                onClick = onDoneClick,
                includePadding = false,
                fontSize = 28.sp,
                modifier = Modifier.padding(top = 40.dp, bottom = 24.dp).width(460.dp),
            )
        }
    } else {
        Box(modifier = background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    DoneHeader(state)
                }
                NewsletterRow(state, onNewsletterChange, Modifier.fillMaxWidth())
                RowButton(
                    text = stringResource(LR.string.done),
                    onClick = onDoneClick,
                    includePadding = false,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }
            CloseButton(
                onClick = onCloseClick,
                tintColor = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.DoneHeader(state: CreateDonePageState) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        state.imageRes?.let {
            Image(painter = painterResource(it), contentDescription = stringResource(LR.string.done), modifier = Modifier.fillMaxSize())
        }
        if (state.isProgressVisible) {
            CircularProgressIndicator(color = MaterialTheme.theme.colors.primaryInteractive01)
        }
    }
    TextH20(
        text = state.title,
        textAlign = TextAlign.Center,
        fontSize = if (state.isCar) 36.sp else 22.sp,
        lineHeight = if (state.isCar) 44.sp else 30.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
    )
    TextH40(
        text = state.detail,
        color = MaterialTheme.theme.colors.primaryText02,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
    )
}

@Composable
private fun NewsletterRow(state: CreateDonePageState, onNewsletterChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingRow(
        primaryText = stringResource(LR.string.profile_pocket_casts_newsletter),
        secondaryText = stringResource(LR.string.profile_create_newsletter_summary),
        icon = painterResource(R.drawable.ic_newsletter),
        toggle = SettingRowToggle.Switch(checked = state.isNewsletterChecked),
        modifier = modifier.toggleable(
            value = state.isNewsletterChecked,
            role = Role.Switch,
            onValueChange = onNewsletterChange,
        ),
    )
}
