package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun EpisodeDurationRulePage(
    isDurationConstrained: Boolean,
    minDuration: Duration,
    maxDuration: Duration,
    onChangeConstrainDuration: (Boolean) -> Unit,
    onDecrementMinDuration: () -> Unit,
    onIncrementMinDuration: () -> Unit,
    onDecrementMaxDuration: () -> Unit,
    onIncrementMaxDuration: () -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.filters_episode_duration),
        isSaveEnabled = maxDuration > minDuration,
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = bottomPadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .toggleable(
                        value = isDurationConstrained,
                        role = Role.Switch,
                        onValueChange = onChangeConstrainDuration,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.filters_filter_by_duration),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isDurationConstrained,
                    onCheckedChange = null,
                )
            }
            DurtionRow(
                label = stringResource(LR.string.filters_duration_longer_than),
                duration = minDuration,
                isEnabled = isDurationConstrained,
                decrementDescription = stringResource(LR.string.decrement_longer_than_duration),
                incrementDescription = stringResource(LR.string.increment_longer_than_duration),
                onDecrement = onDecrementMinDuration,
                onIncrement = onIncrementMinDuration,
            )
            DurtionRow(
                label = stringResource(LR.string.filters_duration_shorter_than),
                duration = maxDuration,
                isEnabled = isDurationConstrained,
                decrementDescription = stringResource(LR.string.decrement_shorter_than_duration),
                incrementDescription = stringResource(LR.string.increment_shorter_than_duration),
                onDecrement = onDecrementMaxDuration,
                onIncrement = onIncrementMaxDuration,
            )
        }
    }
}

@Composable
private fun DurtionRow(
    label: String,
    duration: Duration,
    isEnabled: Boolean,
    decrementDescription: String,
    incrementDescription: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alpha by animateFloatAsState(if (isEnabled) 1f else 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .alpha(alpha)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        TextH40(
            text = label,
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        TextH40(
            text = TimeHelper.getTimeDurationShortString(
                context = context,
                timeMs = duration.inWholeMilliseconds,
            ),
        )
        Spacer(
            modifier = Modifier.width(4.dp),
        )
        IconButton(
            onClick = onDecrement,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_minus),
                contentDescription = decrementDescription,
                tint = MaterialTheme.theme.colors.primaryIcon01,
            )
        }
        IconButton(
            onClick = onIncrement,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_effects_plus),
                contentDescription = incrementDescription,
                tint = MaterialTheme.theme.colors.primaryIcon01,
            )
        }
    }
}

@Composable
@PreviewRegularDevice
private fun EpisodeDurationRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var isConstrained by remember { mutableStateOf(true) }
    var minDuration by remember { mutableStateOf(20.minutes) }
    var maxDuration by remember { mutableStateOf(40.minutes) }

    AppThemeWithBackground(themeType) {
        EpisodeDurationRulePage(
            isDurationConstrained = isConstrained,
            minDuration = minDuration,
            maxDuration = maxDuration,
            onChangeConstrainDuration = { isConstrained = it },
            onDecrementMinDuration = { minDuration -= 5.minutes },
            onIncrementMinDuration = { minDuration += 5.minutes },
            onDecrementMaxDuration = { maxDuration -= 5.minutes },
            onIncrementMaxDuration = { maxDuration += 5.minutes },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
