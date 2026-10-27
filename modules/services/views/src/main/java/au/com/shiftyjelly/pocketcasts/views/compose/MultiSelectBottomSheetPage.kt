package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodeAction
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun MultiSelectBottomSheetPage(
    title: String,
    actions: List<MultiSelectAction>,
    onEditClick: () -> Unit,
    onActionClick: (MultiSelectAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            DrawableIcon(
                drawableId = R.drawable.background_dragger,
                tint = null,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .align(Alignment.TopCenter),
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                TextViewLine(
                    text = AnnotatedString(title),
                    size = 18.sp,
                    color = MaterialTheme.theme.colors.primaryText02,
                    isMedium = true,
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Box(modifier = Modifier.matchParentSize()) {
                    EditButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .requiredSize(44.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(top = 16.dp)) {
            actions.forEach { action ->
                MultiSelectActionRow(
                    action = action,
                    modifier = Modifier.clickable { onActionClick(action) },
                )
            }
        }
    }
}

@Composable
private fun EditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable(
            interactionSource = null,
            indication = ripple(bounded = false),
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        DrawableIcon(
            drawableId = IR.drawable.ic_edit,
            tint = MaterialTheme.theme.colors.primaryIcon01,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center),
        )
    }
}

@Preview
@Composable
private fun MultiSelectBottomSheetPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        MultiSelectBottomSheetPage(
            title = "3 selected episodes",
            actions = remember { MultiSelectEpisodeAction.listFromIds(emptyList()).takeLast(4) + MultiSelectAction.SelectAll },
            onEditClick = {},
            onActionClick = {},
        )
    }
}
