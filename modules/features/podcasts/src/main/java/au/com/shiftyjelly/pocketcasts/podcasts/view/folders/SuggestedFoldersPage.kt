package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFoldersPage(
    onDismiss: () -> Unit,
    onUseTheseFolders: () -> Unit,
    onCreateCustomFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )

        Image(
            painter = painterResource(IR.drawable.ic_close),
            contentDescription = stringResource(LR.string.close),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryInteractive01),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(24.dp)
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = ripple(color = Color.Black, bounded = false),
                    onClickLabel = stringResource(LR.string.close),
                    role = Role.Button,
                    onClick = onDismiss,
                ),
        )

        TextH10(
            text = stringResource(LR.string.suggested_folders),
            lineHeight = 36.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        TextP40(
            text = stringResource(LR.string.suggested_folders_subtitle),
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        RowButton(
            text = stringResource(LR.string.suggested_folders_use_these_folders_button),
            modifier = Modifier.padding(bottom = 16.dp),
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            ),
            includePadding = false,
            onClick = onUseTheseFolders,
        )

        RowOutlinedButton(
            text = stringResource(id = LR.string.suggested_folders_use_create_custom_folders_button),
            onClick = onCreateCustomFolders,
            includePadding = false,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryIcon01, backgroundColor = Color.Transparent),
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestedFoldersPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        SuggestedFoldersPage(
            onDismiss = {},
            onUseTheseFolders = {},
            onCreateCustomFolders = {},
        )
    }
}
