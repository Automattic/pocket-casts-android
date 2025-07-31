package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeDurationRulePage(
    isDurationConstrained: Boolean,
    minDuration: Duration,
    maxDuration: Duration,
    onToggleConstrainDuration: (Boolean) -> Unit,
    onDecrementMinDuration: () -> Unit,
    onIncrementMinDuration: () -> Unit,
    onDecrementMaxDuration: () -> Unit,
    onIncrementMaxDuration: () -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(
            onClick = onClickBack,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(LR.string.back),
                tint = MaterialTheme.theme.colors.primaryIcon03,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH20(
                text = stringResource(LR.string.filters_episode_duration),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            RowButton(
                text = stringResource(LR.string.save_smart_rule),
                enabled = if (isDurationConstrained) {
                    maxDuration >= minDuration
                } else {
                    true
                },
                onClick = onSaveRule,
                includePadding = false,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun EpisodeDurationRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var isConstrained by remember { mutableStateOf(false) }
    var minDuration by remember { mutableStateOf(20.minutes) }
    var maxDuration by remember { mutableStateOf(40.minutes) }

    AppThemeWithBackground(themeType) {
        EpisodeDurationRulePage(
            isDurationConstrained = isConstrained,
            minDuration = minDuration,
            maxDuration = maxDuration,
            onToggleConstrainDuration = { isConstrained = it },
            onDecrementMinDuration = { minDuration -= 5.minutes },
            onIncrementMinDuration = { minDuration += 5.minutes },
            onDecrementMaxDuration = { maxDuration -= 5.minutes },
            onIncrementMaxDuration = { maxDuration += 5.minutes },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
