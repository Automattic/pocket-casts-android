package au.com.shiftyjelly.pocketcasts.settings.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralEpisodes
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun DiskSpaceSizeView(
    diskSpaceView: ManualCleanupViewModel.State.DiskSpaceView,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 16.dp)
    ) {
        TextH40(
            text = stringResource(diskSpaceView.title),
            modifier = modifier.weight(1f),
        )
        TextC70(
            text = getFormattedSubtitle(diskSpaceView, context),
            modifier = modifier.padding(start = 16.dp),
        )
        Checkbox(
            checked = diskSpaceView.isChecked,
            onCheckedChange = { onCheckedChange(it) },
        )
    }
}
private fun getFormattedSubtitle(
    diskSpaceView: ManualCleanupViewModel.State.DiskSpaceView,
    context: Context
): String {
    val byteString = Util.formattedBytes(bytes = diskSpaceView.episodesBytesSize, context = context)
    return if (diskSpaceView.episodes.isEmpty()) {
        context.resources.getStringPluralEpisodes(0)
    } else {
        "${context.resources.getStringPluralEpisodes(diskSpaceView.episodesSize)} · $byteString"
    }
}

@Preview(showBackground = true)
@Composable
private fun DiskSpaceSizeViewPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        DiskSpaceSizeView(
            diskSpaceView = ManualCleanupViewModel.State.DiskSpaceView(title = LR.string.unplayed),
            onCheckedChange = { _ -> },
        )
    }
}
