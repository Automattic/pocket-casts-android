package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.account.R as AR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun SubCancelledPage(
    expiryHint: String,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .nestedScroll(rememberNestedScrollInteropConnection())
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Image(
                painter = painterResource(AR.drawable.ic_subscription_cancelled),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                    .size(140.dp),
            )
            TextH30(
                text = stringResource(LR.string.profile_sub_sorry_to_see_you_go),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 32.dp, top = 32.dp, end = 32.dp)
                    .fillMaxWidth(),
            )
            TextP30(
                text = stringResource(LR.string.profile_sub_thankyou_for_support),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 32.dp, top = 8.dp, end = 32.dp, bottom = 64.dp)
                    .fillMaxWidth(),
            )
            NotesCard(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            ) {
                NotesCardRow(text = expiryHint) {
                    NoteIcon(IR.drawable.ic_subscription)
                }
                NotesCardRow(text = stringResource(LR.string.profile_sub_cancel_hint1)) {
                    NoteIcon(IR.drawable.ic_locked_large, tint = colorResource(UR.color.yellow_50))
                }
                NotesCardRow(text = stringResource(LR.string.profile_sub_cancel_hint2)) {
                    NoteIcon(R.drawable.ic_cloud_strikethrough)
                }
            }
        }
        RowButton(
            text = stringResource(LR.string.done),
            onClick = onDoneClick,
            includePadding = false,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun NoteIcon(
    @DrawableRes iconRes: Int,
    tint: Color = Color.Unspecified,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(LR.string.profile_plus),
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

@Preview
@Composable
private fun SubCancelledPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        SubCancelledPage(
            expiryHint = "",
            onDoneClick = {},
        )
    }
}
