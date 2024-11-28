package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR

private val DefaultCardShape = RoundedCornerShape(8.dp)

private object NotesCardTheme {
    val backgroundColor @Composable get() = MaterialTheme.theme.colors.primaryUi01
    val borderColor @Composable get() = MaterialTheme.theme.colors.primaryText02
}

@Composable
internal fun TrialFinishedNotesCard(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            shape = DefaultCardShape,
            elevation = 8.dp,
            backgroundColor = NotesCardTheme.backgroundColor,
            modifier = Modifier
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
}

@Composable
private fun NotesItemRow(
    item: NotesItem,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    ) {
        GradientIcon(
            painter = painterResource(item.icon),
            gradientBrush = plusGradientBrush,
            modifier = Modifier
                .padding(start = 32.dp)
                .size(24.dp),
        )

        TextH50(
            text = stringResource(item.text),
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotesCardPreview() {
    TrialFinishedNotesCard()
}

internal enum class TrialFinishedNotesItem(
    override val icon: Int,
    override val iconContentDescription: Int,
    override val text: Int,
) : NotesItem {
    LOCK_FEATURE(
        text = LR.string.plus_trial_finished_locked,
        icon = PR.drawable.ic_locked_large,
        iconContentDescription = LR.string.profile_plus,
    ),
    FILES_REMOVED(
        text = LR.string.plus_trial_finished_files_removed,
        icon = IR.drawable.ic_cloud,
        iconContentDescription = LR.string.profile_plus,
    ),
    UPGRADE_ACCOUNT(
        text = LR.string.plus_trial_finished_continue,
        icon = IR.drawable.ic_plus,
        iconContentDescription = LR.string.profile_plus,
    ),
}

interface NotesItem {
    @get:DrawableRes
    val icon: Int

    @get:StringRes
    val iconContentDescription: Int

    @get:StringRes
    val text: Int
}
