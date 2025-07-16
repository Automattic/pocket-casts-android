package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val DefaultCardShape = RoundedCornerShape(8.dp)

private object NotesCardTheme {
    val backgroundColor @Composable get() = MaterialTheme.theme.colors.primaryUi01
    val borderColor @Composable get() = MaterialTheme.theme.colors.primaryText02
}

@Composable
internal fun TrialFinishedNotesCard(
    modifier: Modifier = Modifier,
) {
    Card(
        shape = DefaultCardShape,
        elevation = 8.dp,
        backgroundColor = NotesCardTheme.backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = NotesCardTheme.borderColor, shape = DefaultCardShape),
    ) {
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            TrialFinishedNotesItem.entries.forEach {
                NotesItemRow(item = it)
            }
        }
    }
}

@Composable
private fun NotesItemRow(
    item: TrialFinishedNotesItem,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 32.dp, end = 16.dp),
    ) {
        GradientIcon(
            painter = painterResource(item.icon),
            gradientBrush = Brush.plusGradientBrush,
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.width(16.dp))

        TextH50(
            text = stringResource(item.text),
            color = MaterialTheme.theme.colors.primaryText02,
            fontWeight = FontWeight.W400,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesCardPreview() {
    TrialFinishedNotesCard()
}

private enum class TrialFinishedNotesItem(
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
) {
    LOCK_FEATURE(
        text = LR.string.plus_trial_finished_locked,
        icon = IR.drawable.ic_locked_large,
    ),
    FILES_REMOVED(
        text = LR.string.plus_trial_finished_files_removed,
        icon = IR.drawable.ic_cloud,
    ),
    UPGRADE_ACCOUNT(
        text = LR.string.plus_trial_finished_continue,
        icon = IR.drawable.ic_plus,
    ),
}
